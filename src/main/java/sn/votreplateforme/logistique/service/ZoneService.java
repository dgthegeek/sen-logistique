package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.repository.QuartierRepository;
import sn.votreplateforme.logistique.repository.ZoneRepository;
import sn.votreplateforme.logistique.util.TarifCalculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des zones et quartiers
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final QuartierRepository quartierRepository;
    private final TarifCalculator tarifCalculator;

    /**
     * Récupère toutes les zones actives avec leurs communes
     */
    public List<Zone> getAllZones() {
        log.info("Récupération de toutes les zones actives");

        List<sn.votreplateforme.logistique.entity.Zone> zones =
                zoneRepository.findByActiveTrue();

        return zones.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un quartier par son nom et sa commune
     */
    public sn.votreplateforme.logistique.entity.Quartier getQuartierByNomAndCommune(String nom, String commune) {
        log.debug("Recherche du quartier {} dans la commune {}", nom, commune);
        return quartierRepository.findByNomAndCommune(nom, commune)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Quartier '%s' non trouvé dans la commune '%s'", nom, commune)
                ));
    }

    /**
     * Trouve la zone d'un quartier
     */
    public sn.votreplateforme.logistique.entity.Zone findZoneByQuartier(String quartierNom, String commune) {
        log.debug("Recherche de la zone pour {} ({})", quartierNom, commune);

        sn.votreplateforme.logistique.entity.Quartier quartier = getQuartierByNomAndCommune(quartierNom, commune);

        if (quartier.getZone() == null) {
            throw new IllegalStateException(
                    String.format("Le quartier '%s' n'a pas de zone associée", quartierNom)
            );
        }

        return quartier.getZone();
    }

    /**
     * Récupère les quartiers d'une commune
     *
     * @param commune Nom de la commune (ex: "Dakar")
     */
    public List<Quartier> getQuartiersByCommune(String commune) {
        log.info("Récupération des quartiers de : {}", commune);

        List<sn.votreplateforme.logistique.entity.Quartier> quartiers =
                quartierRepository.findByCommuneAndActifTrue(commune);

        return quartiers.stream()
                .map(this::mapQuartierToDto)
                .collect(Collectors.toList());
    }

    /**
     * Calcule le tarif de livraison
     *
     * @param zoneId ID de la zone
     * @param urgenceDto Type d'urgence (NORMAL ou EXPRESS)
     * @param poids Poids du colis en kg (optionnel)
     */
    public TarifResponse calculerTarif(Long zoneId, sn.votreplateforme.logistique.dto.TypeUrgence urgenceDto, Double poids) {
        log.info("Calcul tarif - zoneId: {}, urgence: {}, poids: {}", zoneId, urgenceDto, poids);

        // Récupérer la zone
        sn.votreplateforme.logistique.entity.Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone non trouvée avec l'ID : " + zoneId));

        // CONVERSION : DTO TypeUrgence → Entity TypeUrgence
        sn.votreplateforme.logistique.entity.TypeUrgence urgenceEntity =
                convertDtoUrgenceToEntity(urgenceDto);

        // Calculer le tarif avec l'enum Entity
        BigDecimal tarif = tarifCalculator.calculer(zone, urgenceEntity, poids);

        // Construire la réponse avec les détails du calcul
        TarifResponse response = new TarifResponse();
        response.setMontant(tarif);
        response.setZone(zone.getNom());
        response.setUrgence(urgenceDto); // On retourne le DTO pour la réponse

        // Détails du calcul
        TarifResponseDetailCalcul detailCalcul = new TarifResponseDetailCalcul();

        // Tarif de base selon l'urgence
        BigDecimal tarifBase = urgenceEntity == sn.votreplateforme.logistique.entity.TypeUrgence.EXPRESS ?
                zone.getTarifExpress() : zone.getTarifStandard();
        detailCalcul.setTarifBase(tarifBase);

        // Supplément poids
        BigDecimal supplementPoids = BigDecimal.ZERO;
        if (poids != null) {
            if (poids > 10) {
                supplementPoids = new BigDecimal("1000");
            } else if (poids > 5) {
                supplementPoids = new BigDecimal("500");
            }
        }
        detailCalcul.setSupplementPoids(supplementPoids);

        // Supplément urgence (déjà inclus dans tarifBase)
        BigDecimal supplementUrgence = urgenceEntity == sn.votreplateforme.logistique.entity.TypeUrgence.EXPRESS ?
                zone.getTarifExpress().subtract(zone.getTarifStandard()) : BigDecimal.ZERO;
        detailCalcul.setSupplementUrgence(supplementUrgence);

        response.setDetailCalcul(detailCalcul);

        log.info("Tarif calculé : {} FCFA (base: {}, poids: {})",
                tarif, tarifBase, supplementPoids);

        return response;
    }


    /**
     * Trouve une zone par son ID
     */
    public sn.votreplateforme.logistique.entity.Zone findById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone non trouvée avec l'ID : " + id));
    }

    // ============================================
    // MAPPERS
    // ============================================

    /**
     * Mapper Entity Zone → DTO Zone (AVEC LES COMMUNES)
     */
    private Zone mapToDto(sn.votreplateforme.logistique.entity.Zone entity) {
        Zone dto = new Zone();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setDescription(entity.getDescription());
        dto.setTarifStandard(entity.getTarifStandard());
        dto.setTarifExpress(entity.getTarifExpress());

        List<String> communes = entity.getQuartiers().stream()
                .map(sn.votreplateforme.logistique.entity.Quartier::getCommune)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        dto.setCommunes(communes);

        return dto;
    }

    /**
     * Mapper Entity Quartier → DTO Quartier
     */
    private Quartier mapQuartierToDto(sn.votreplateforme.logistique.entity.Quartier entity) {
        Quartier dto = new Quartier();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setCommune(entity.getCommune());

        // Mapper la zone aussi
        if (entity.getZone() != null) {
            dto.setZone(mapToDto(entity.getZone()));
        }

        return dto;
    }

    /**
     * Convertit un TypeUrgence DTO en TypeUrgence Entity
     */
    private sn.votreplateforme.logistique.entity.TypeUrgence convertDtoUrgenceToEntity(
            sn.votreplateforme.logistique.dto.TypeUrgence dtoUrgence) {

        if (dtoUrgence == null) {
            return sn.votreplateforme.logistique.entity.TypeUrgence.NORMAL;
        }

        return switch (dtoUrgence) {
            case NORMAL -> sn.votreplateforme.logistique.entity.TypeUrgence.NORMAL;
            case EXPRESS -> sn.votreplateforme.logistique.entity.TypeUrgence.EXPRESS;
        };
    }
}