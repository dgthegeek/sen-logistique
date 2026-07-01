package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.TransactionRepository;

import java.math.BigDecimal;

/**
 * Source unique de vérité pour les calculs financiers d'un vendeur.
 *
 * <p>Tous les montants sont dérivés dynamiquement des livraisons et des paiements,
 * jamais d'un champ stocké susceptible de devenir obsolète :
 * <ul>
 *   <li>{@code chiffreAffaires} = Σ(montantCOD − fraisLivraison) des livraisons LIVREE (cumulé, jamais remis à zéro).</li>
 *   <li>{@code totalPaye} = Σ des paiements PAIEMENT_VENDEUR au statut EFFECTUE.</li>
 *   <li>{@code soldeDisponible} = chiffreAffaires − totalPaye (l'argent que la plateforme doit encore au vendeur).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinanceCalculator {

    private final LivraisonRepository livraisonRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Chiffre d'affaires cumulé du vendeur : prix produit (hors frais de livraison)
     * de toutes ses livraisons livrées. N'est jamais remis à zéro.
     */
    public BigDecimal chiffreAffaires(Vendeur vendeur) {
        BigDecimal ca = livraisonRepository.sumProduitLivrees(vendeur);
        return ca != null ? ca : BigDecimal.ZERO;
    }

    /** Total déjà versé au vendeur (paiements EFFECTUE). */
    public BigDecimal totalPaye(Vendeur vendeur) {
        BigDecimal paye = transactionRepository.sumPaiementsEffectues(vendeur);
        return paye != null ? paye : BigDecimal.ZERO;
    }

    /**
     * Solde disponible = argent que la plateforme doit encore au vendeur
     * = chiffre d'affaires cumulé − total déjà payé.
     */
    public BigDecimal soldeDisponible(Vendeur vendeur) {
        return chiffreAffaires(vendeur).subtract(totalPaye(vendeur));
    }
}
