package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Quartier;
import sn.votreplateforme.logistique.entity.Zone;
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.QuartierRepository;
import sn.votreplateforme.logistique.repository.ZoneRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneAdminService {

    private final ZoneRepository zoneRepository;
    private final QuartierRepository quartierRepository;
    private final LivraisonRepository livraisonRepository;

    /**
     * Liste toutes les zones avec filtres et pagination
     */
    @Transactional(readOnly = true)
    public PageZone getAllZones(Boolean actif, String search, Integer page, Integer size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "nom");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Zone> zonesPage = zoneRepository.searchZones(actif, search, pageable);

        PageZone pageZone = new PageZone();
        pageZone.setContent(zonesPage.getContent().stream()
                .map(this::toZoneAdminDTO)
                .toList());
        pageZone.setPage(zonesPage.getNumber());
        pageZone.setSize(zonesPage.getSize());
        pageZone.setTotalElements((int) zonesPage.getTotalElements());
        pageZone.setTotalPages(zonesPage.getTotalPages());

        return pageZone;
    }

    /**
     * Détails d'une zone avec ses quartiers
     */
    @Transactional(readOnly = true)
    public ZoneDetailDTO getZoneDetail(Long zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone non trouvée"));

        ZoneDetailDTO dto = new ZoneDetailDTO();
        dto.setId(zone.getId());
        dto.setNom(zone.getNom());
        dto.setDescription(zone.getDescription());
        dto.setTarifStandard(zone.getTarifStandard());
        dto.setTarifExpress(zone.getTarifExpress());
        dto.setActif(zone.isActive());

        // Liste des quartiers
        List<Quartier> quartiers = quartierRepository.findByZoneId(zoneId);
        dto.setQuartiers(quartiers.stream()
                .map(q -> {
                    ZoneDetailDTOQuartiersInner quartierDTO = new ZoneDetailDTOQuartiersInner();
                    quartierDTO.setId(q.getId());
                    quartierDTO.setNom(q.getNom());
                    quartierDTO.setCommune(q.getCommune());
                    quartierDTO.setActif(q.isActif());
                    return quartierDTO;
                })
                .toList());

        return dto;
    }

    /**
     * Créer une nouvelle zone
     */
    @Transactional
    public AdminZonesPost201Response createZone(CreateZoneRequest request) {
        // Vérifier que le nom n'existe pas déjà
        if (zoneRepository.findByNom(request.getNom()).isPresent()) {
            throw new BadRequestException("Une zone avec ce nom existe déjà");
        }

        // Vérifier que tarifExpress > tarifStandard
        if (request.getTarifExpress().compareTo(request.getTarifStandard()) <= 0) {
            throw new BadRequestException("Le tarif express doit être supérieur au tarif standard");
        }

        Zone zone = new Zone();
        zone.setNom(request.getNom());
        zone.setDescription(request.getDescription());
        zone.setTarifStandard(request.getTarifStandard());
        zone.setTarifExpress(request.getTarifExpress());
        zone.setActive(request.getActif());

        zone = zoneRepository.save(zone);

        log.info("Zone créée : {} (ID: {})", zone.getNom(), zone.getId());

        AdminZonesPost201Response response = new AdminZonesPost201Response();
        response.setMessage("Zone créée avec succès");
        response.setZone(toZoneAdminDTO(zone));

        return response;
    }

    /**
     * Modifier une zone
     */
    @Transactional
    public AdminZonesIdPut200Response updateZone(Long zoneId, UpdateZoneRequest request) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone non trouvée"));

        // Vérifier unicité du nom (sauf si c'est le même)
        zoneRepository.findByNom(request.getNom())
                .ifPresent(z -> {
                    if (!z.getId().equals(zoneId)) {
                        throw new BadRequestException("Une zone avec ce nom existe déjà");
                    }
                });

        // Vérifier que tarifExpress > tarifStandard
        if (request.getTarifExpress().compareTo(request.getTarifStandard()) <= 0) {
            throw new BadRequestException("Le tarif express doit être supérieur au tarif standard");
        }

        zone.setNom(request.getNom());
        zone.setDescription(request.getDescription());
        zone.setTarifStandard(request.getTarifStandard());
        zone.setTarifExpress(request.getTarifExpress());
        zone.setActive(request.getActif());

        zone = zoneRepository.save(zone);

        log.info("Zone modifiée : {} (ID: {})", zone.getNom(), zone.getId());

        AdminZonesIdPut200Response response = new AdminZonesIdPut200Response();
        response.setMessage("Zone modifiée avec succès");
        response.setZone(toZoneAdminDTO(zone));

        return response;
    }

    /**
     * Modifier uniquement les tarifs
     */
    @Transactional
    public AdminZonesIdTarifsPatch200Response updateTarifs(Long zoneId, UpdateTarifsRequest request) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone non trouvée"));

        // Vérifier que tarifExpress > tarifStandard
        if (request.getTarifExpress().compareTo(request.getTarifStandard()) <= 0) {
            throw new BadRequestException("Le tarif express doit être supérieur au tarif standard");
        }

        zone.setTarifStandard(request.getTarifStandard());
        zone.setTarifExpress(request.getTarifExpress());

        zone = zoneRepository.save(zone);

        log.info("Tarifs modifiés pour zone : {} - Standard: {}, Express: {}",
                zone.getNom(), zone.getTarifStandard(), zone.getTarifExpress());

        AdminZonesIdTarifsPatch200Response response = new AdminZonesIdTarifsPatch200Response();
        response.setMessage("Tarifs mis à jour avec succès");

        AdminZonesIdTarifsPatch200ResponseZone zoneDTO = new AdminZonesIdTarifsPatch200ResponseZone();
        zoneDTO.setId(zone.getId());
        zoneDTO.setNom(zone.getNom());
        zoneDTO.setTarifStandard(zone.getTarifStandard());
        zoneDTO.setTarifExpress(zone.getTarifExpress());

        response.setZone(zoneDTO);

        return response;
    }

    /**
     * Activer/Désactiver une zone
     */
    @Transactional
    public AdminZonesIdTogglePatch200Response toggleZone(Long zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone non trouvée"));

        zone.setActive(!zone.isActive());
        zone = zoneRepository.save(zone);

        String action = zone.isActive() ? "activée" : "désactivée";
        log.info("Zone {} : {}", action, zone.getNom());

        AdminZonesIdTogglePatch200Response response = new AdminZonesIdTogglePatch200Response();
        response.setMessage("Zone " + action + " avec succès");

        AdminZonesIdTogglePatch200ResponseZone zoneDTO = new AdminZonesIdTogglePatch200ResponseZone();
        zoneDTO.setId(zone.getId());
        zoneDTO.setNom(zone.getNom());
        zoneDTO.setActif(zone.isActive());

        response.setZone(zoneDTO);

        return response;
    }

    /**
     * Supprimer une zone
     */
    @Transactional
    public void deleteZone(Long zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone non trouvée"));

        // Vérifier qu'il n'y a pas de quartiers
        long nombreQuartiers = quartierRepository.countByZoneId(zoneId);
        if (nombreQuartiers > 0) {
            throw new BadRequestException(
                    "Impossible de supprimer la zone : " + nombreQuartiers + " quartiers y sont associés"
            );
        }

        // Vérifier qu'il n'y a pas de livraisons
        long nombreLivraisons = livraisonRepository.countByAdresseDestination_ZoneId(zoneId);
        if (nombreLivraisons > 0) {
            throw new BadRequestException(
                    "Impossible de supprimer la zone : " + nombreLivraisons + " livraisons y sont associées"
            );
        }

        zoneRepository.delete(zone);

        log.warn("Zone supprimée : {} (ID: {})", zone.getNom(), zone.getId());
    }

    // ===== MÉTHODES PRIVÉES =====

    private ZoneAdminDTO toZoneAdminDTO(Zone zone) {
        ZoneAdminDTO dto = new ZoneAdminDTO();
        dto.setId(zone.getId());
        dto.setNom(zone.getNom());
        dto.setDescription(zone.getDescription());
        dto.setTarifStandard(zone.getTarifStandard());
        dto.setTarifExpress(zone.getTarifExpress());
        dto.setActif(zone.isActive());

        // Compter les quartiers
        long nombreQuartiers = quartierRepository.countByZoneId(zone.getId());
        dto.setNombreQuartiers((int) nombreQuartiers);

        return dto;
    }
}