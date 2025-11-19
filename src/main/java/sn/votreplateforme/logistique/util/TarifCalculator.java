package sn.votreplateforme.logistique.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sn.votreplateforme.logistique.entity.TypeUrgence;
import sn.votreplateforme.logistique.entity.Zone;

import java.math.BigDecimal;

/**
 * Calculateur de tarifs de livraison
 * Calcule les frais selon la zone, l'urgence et le poids
 */
@Component
@Slf4j
public class TarifCalculator {
    
    // Seuils de poids (en kg)
    private static final double POIDS_LEGER = 5.0;
    private static final double POIDS_MOYEN = 10.0;
    
    // Suppléments de poids (en FCFA)
    private static final BigDecimal SUPPLEMENT_POIDS_MOYEN = new BigDecimal("500");  // 5-10 kg
    private static final BigDecimal SUPPLEMENT_POIDS_LOURD = new BigDecimal("1000"); // > 10 kg
    
    // Multiplicateur pour EXPRESS
    private static final BigDecimal MULTIPLICATEUR_EXPRESS = new BigDecimal("1.5");
    
    /**
     * Calcule le tarif de livraison
     * 
     * @param zone Zone de livraison
     * @param urgence Type d'urgence (NORMAL ou EXPRESS)
     * @param poids Poids du colis en kg (optionnel)
     * @return Montant des frais de livraison
     */
    public BigDecimal calculer(Zone zone, TypeUrgence urgence, Double poids) {
        log.debug("Calcul du tarif - Zone: {}, Urgence: {}, Poids: {}", 
            zone.getNom(), urgence, poids);
        
        // 1. Tarif de base selon la zone et l'urgence
        BigDecimal tarifBase;
        if (urgence == TypeUrgence.EXPRESS) {
            tarifBase = zone.getTarifExpress();
        } else {
            tarifBase = zone.getTarifStandard();
        }
        
        log.debug("Tarif de base: {} FCFA", tarifBase);
        
        // 2. Supplément selon le poids (si fourni)
        BigDecimal supplementPoids = BigDecimal.ZERO;
        if (poids != null && poids > 0) {
            if (poids > POIDS_MOYEN) {
                // Colis lourd (> 10 kg)
                supplementPoids = SUPPLEMENT_POIDS_LOURD;
                log.debug("Supplément poids lourd (>10kg): {} FCFA", supplementPoids);
            } else if (poids > POIDS_LEGER) {
                // Colis moyen (5-10 kg)
                supplementPoids = SUPPLEMENT_POIDS_MOYEN;
                log.debug("Supplément poids moyen (5-10kg): {} FCFA", supplementPoids);
            }
            // Sinon poids léger (<5 kg) = pas de supplément
        }
        
        // 3. Calcul du tarif final
        BigDecimal tarifFinal = tarifBase.add(supplementPoids);
        
        log.info("Tarif final calculé: {} FCFA (base: {}, supplément poids: {})", 
            tarifFinal, tarifBase, supplementPoids);
        
        return tarifFinal;
    }
    
    /**
     * Calcule le tarif avec détails pour affichage
     * 
     * @return Objet avec les détails du calcul
     */
    public TarifDetail calculerAvecDetails(Zone zone, TypeUrgence urgence, Double poids) {
        BigDecimal tarifBase;
        if (urgence == TypeUrgence.EXPRESS) {
            tarifBase = zone.getTarifExpress();
        } else {
            tarifBase = zone.getTarifStandard();
        }
        
        BigDecimal supplementPoids = BigDecimal.ZERO;
        if (poids != null && poids > 0) {
            if (poids > POIDS_MOYEN) {
                supplementPoids = SUPPLEMENT_POIDS_LOURD;
            } else if (poids > POIDS_LEGER) {
                supplementPoids = SUPPLEMENT_POIDS_MOYEN;
            }
        }
        
        BigDecimal tarifFinal = tarifBase.add(supplementPoids);
        
        return new TarifDetail(tarifBase, supplementPoids, BigDecimal.ZERO, tarifFinal);
    }
    
    /**
     * Classe pour les détails du calcul de tarif
     */
    public static class TarifDetail {
        public final BigDecimal tarifBase;
        public final BigDecimal supplementPoids;
        public final BigDecimal supplementUrgence;
        public final BigDecimal montantTotal;
        
        public TarifDetail(BigDecimal tarifBase, BigDecimal supplementPoids, 
                          BigDecimal supplementUrgence, BigDecimal montantTotal) {
            this.tarifBase = tarifBase;
            this.supplementPoids = supplementPoids;
            this.supplementUrgence = supplementUrgence;
            this.montantTotal = montantTotal;
        }
    }
}
