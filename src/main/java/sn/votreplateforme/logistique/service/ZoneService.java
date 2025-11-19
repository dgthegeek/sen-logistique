package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.entity.Quartier;
import sn.votreplateforme.logistique.entity.Zone;
import sn.votreplateforme.logistique.repository.QuartierRepository;
import sn.votreplateforme.logistique.repository.ZoneRepository;

import java.util.List;

/**
 * Service de gestion des zones et quartiers
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {
    
    private final ZoneRepository zoneRepository;
    private final QuartierRepository quartierRepository;
    
    /**
     * Récupère toutes les zones actives
     */
    public List<Zone> getAllZones() {
        log.debug("Récupération de toutes les zones actives");
        return zoneRepository.findByActiveTrue();
    }
    
    /**
     * Récupère une zone par son ID
     */
    public Zone getZoneById(Long zoneId) {
        log.debug("Récupération de la zone ID: {}", zoneId);
        return zoneRepository.findById(zoneId)
            .orElseThrow(() -> new IllegalArgumentException("Zone non trouvée avec l'ID: " + zoneId));
    }
    
    /**
     * Récupère tous les quartiers d'une commune
     */
    public List<Quartier> getQuartiersByCommune(String commune) {
        log.debug("Récupération des quartiers de la commune: {}", commune);
        return quartierRepository.findByCommuneAndActifTrue(commune);
    }
    
    /**
     * Récupère un quartier par son nom et sa commune
     */
    public Quartier getQuartierByNomAndCommune(String nom, String commune) {
        log.debug("Recherche du quartier {} dans la commune {}", nom, commune);
        return quartierRepository.findByNomAndCommune(nom, commune)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Quartier '%s' non trouvé dans la commune '%s'", nom, commune)
            ));
    }
    
    /**
     * Trouve la zone d'un quartier
     */
    public Zone findZoneByQuartier(String quartierNom, String commune) {
        log.debug("Recherche de la zone pour {} ({})", quartierNom, commune);
        
        Quartier quartier = getQuartierByNomAndCommune(quartierNom, commune);
        
        if (quartier.getZone() == null) {
            throw new IllegalStateException(
                String.format("Le quartier '%s' n'a pas de zone associée", quartierNom)
            );
        }
        
        return quartier.getZone();
    }
    
    /**
     * Récupère tous les quartiers (pour l'auto-complétion)
     */
    public List<Quartier> getAllQuartiers() {
        log.debug("Récupération de tous les quartiers actifs");
        return quartierRepository.findByActifTrue();
    }
    
    /**
     * Recherche de quartiers par nom (pour auto-complétion)
     */
    public List<Quartier> searchQuartiersByNom(String recherche) {
        log.debug("Recherche de quartiers contenant: {}", recherche);
        return quartierRepository.findByNomContainingIgnoreCaseAndActifTrue(recherche);
    }
}
