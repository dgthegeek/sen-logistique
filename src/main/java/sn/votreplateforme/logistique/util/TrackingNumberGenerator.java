package sn.votreplateforme.logistique.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.TrackingNumberCounterRepository;

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
    private final TrackingNumberCounterRepository trackingNumberCounterRepository;

    /**
     * Génère un nouveau numéro de tracking unique
     *
     * Format: DKR-YYYYMMDD-XXXXX
     * - DKR : Préfixe pour Dakar
     * - YYYYMMDD : Date du jour
     * - XXXXX : Numéro séquentiel (00001, 00002, etc.)
     *
     * <p>Le numéro séquentiel vient d'un compteur dédié (table
     * {@code tracking_number_counters}), incrémenté par un UPSERT atomique.
     * L'ancienne version comptait les livraisons du jour puis ajoutait 1 :
     * deux créations concurrentes pouvaient lire le même compte et
     * produire le même numéro, violant la contrainte unique en base.
     */
    public String generate() {
        // 1. Obtenir la date du jour
        LocalDate aujourdHui = LocalDate.now();
        String dateStr = aujourdHui.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 2. Préfixe avec la date
        String prefix = "DKR-" + dateStr + "-";

        // 3. Incrémenter le compteur du jour de façon atomique
        int nextNumber = trackingNumberCounterRepository.incrementerEtRecuperer(aujourdHui);

        // 4. Formater avec des zéros à gauche (5 chiffres)
        String numeroSequentiel = String.format("%05d", nextNumber);

        // 5. Construire le numéro de tracking complet
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
