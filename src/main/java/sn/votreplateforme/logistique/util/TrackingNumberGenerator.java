package sn.votreplateforme.logistique.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sn.votreplateforme.logistique.repository.LivraisonRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Générateur de numéros de tracking uniques
 * Format: DKR-YYYYMMDD-XXXXX
 * Exemple: DKR-20251118-00001
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrackingNumberGenerator {
    
    private final LivraisonRepository livraisonRepository;
    
    /**
     * Génère un nouveau numéro de tracking unique
     * 
     * Format: DKR-YYYYMMDD-XXXXX
     * - DKR : Préfixe pour Dakar
     * - YYYYMMDD : Date du jour
     * - XXXXX : Numéro séquentiel (00001, 00002, etc.)
     */
    public String generate() {
        // 1. Obtenir la date du jour au format YYYYMMDD
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 2. Préfixe avec la date
        String prefix = "DKR-" + dateStr + "-";
        
        // 3. Compter le nombre de livraisons créées aujourd'hui
        long count = livraisonRepository.countByNumeroTrackingStartingWith(prefix);
        
        // 4. Générer le numéro séquentiel (incrémenté de 1)
        long nextNumber = count + 1;
        
        // 5. Formater avec des zéros à gauche (5 chiffres)
        String numeroSequentiel = String.format("%05d", nextNumber);
        
        // 6. Construire le numéro de tracking complet
        String numeroTracking = prefix + numeroSequentiel;
        
        log.debug("Numéro de tracking généré: {}", numeroTracking);
        
        return numeroTracking;
    }
    
    /**
     * Génère un numéro de tracking simplifié (pour la V1 si besoin)
     * Format: DKR-XXXXX
     * Exemple: DKR-00001
     */
    public String generateSimple() {
        String prefix = "DKR-";
        
        // Compter toutes les livraisons
        long count = livraisonRepository.count();
        
        // Incrémenter
        long nextNumber = count + 1;
        
        // Formater avec 5 chiffres
        String numeroSequentiel = String.format("%05d", nextNumber);
        
        String numeroTracking = prefix + numeroSequentiel;
        
        log.debug("Numéro de tracking simple généré: {}", numeroTracking);
        
        return numeroTracking;
    }
}
