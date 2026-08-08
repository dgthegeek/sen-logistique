package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.Produit;
import sn.votreplateforme.logistique.entity.Transaction;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.entity.UserRole;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.CloseurRepository;
import sn.votreplateforme.logistique.repository.DispatcheurRepository;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.LivreurRepository;
import sn.votreplateforme.logistique.repository.ProduitRepository;
import sn.votreplateforme.logistique.repository.TransactionRepository;
import sn.votreplateforme.logistique.repository.UserRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;

import java.util.List;

/**
 * Suppression de données (nettoyage administrateur).
 *
 * <p>Toutes les opérations respectent les contraintes de clés étrangères pour ne
 * rien casser :
 * <ul>
 *   <li><b>Livraison</b> : ses lignes de commande sont supprimées en cascade ;
 *       les mouvements de stock qui la référencent sont déliés (SET NULL).</li>
 *   <li><b>Transaction</b> : suppression directe.</li>
 *   <li><b>Membre staff</b> (closeur / livreur / coordinateur) : les livraisons
 *       rattachées ne sont pas supprimées, leur lien est mis à NULL ; les
 *       versements d'un livreur sont supprimés en cascade.</li>
 *   <li><b>Vendeur</b> : suppression en cascade ordonnée (livraisons →
 *       transactions → produits → compte), car {@code produits.vendeur_id} est en
 *       RESTRICT côté base.</li>
 * </ul>
 * Les comptes <b>admin</b> ne sont jamais supprimables par cet outil, et un admin
 * ne peut pas supprimer son propre compte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final LivraisonRepository livraisonRepository;
    private final TransactionRepository transactionRepository;
    private final ProduitRepository produitRepository;
    private final VendeurRepository vendeurRepository;
    private final CloseurRepository closeurRepository;
    private final LivreurRepository livreurRepository;
    private final DispatcheurRepository dispatcheurRepository;
    private final UserRepository userRepository;

    // ==================== LIVRAISON ====================

    @Transactional
    public String supprimerLivraison(Long id) {
        Livraison l = livraisonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Livraison non trouvée : " + id));
        String tracking = l.getNumeroTracking();
        // Les lignes de commande partent en cascade (JPA orphanRemoval + FK CASCADE),
        // les mouvements de stock sont déliés côté base (ON DELETE SET NULL).
        livraisonRepository.delete(l);
        log.warn("🗑️ Livraison supprimée : {} (id={})", tracking, id);
        return "Livraison " + tracking + " supprimée";
    }

    // ==================== TRANSACTION ====================

    @Transactional
    public String supprimerTransaction(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction non trouvée : " + id));
        String ref = t.getReference();
        transactionRepository.delete(t);
        log.warn("🗑️ Transaction supprimée : {} (id={})", ref, id);
        return "Transaction " + ref + " supprimée";
    }

    // ==================== MEMBRE STAFF ====================

    @Transactional
    public String supprimerMembre(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé : " + userId));

        // Garde-fous
        String moi = SecurityUtils.getCurrentUserTelephone();
        if (moi != null && moi.equals(u.getTelephone())) {
            throw new BadRequestException("Vous ne pouvez pas supprimer votre propre compte");
        }
        UserRole role = u.getRole();
        String nom = u.getNomComplet();

        switch (role) {
            case CLOSEUR -> closeurRepository.deleteById(userId);
            case LIVREUR -> livreurRepository.deleteById(userId); // versements en cascade (FK)
            case DISPATCHEUR -> dispatcheurRepository.deleteById(userId);
            case ADMIN -> throw new BadRequestException("Les comptes administrateur ne peuvent pas être supprimés ici");
            case VENDEUR -> throw new BadRequestException("Utilisez la suppression vendeur (supprime aussi ses données)");
            default -> throw new BadRequestException("Type de compte non supporté");
        }
        log.warn("🗑️ Membre supprimé : {} ({}) id={}", nom, role, userId);
        return nom + " (" + role + ") supprimé";
    }

    // ==================== VENDEUR (cascade) ====================

    /** Aperçu de ce qui sera supprimé avec le vendeur. */
    @Transactional(readOnly = true)
    public int[] impactVendeur(Long id) {
        Vendeur v = vendeurRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé : " + id));
        int nbLivraisons = (int) livraisonRepository.countByVendeur(v);
        int nbTransactions = transactionRepository.findByVendeurOrderByDateTransactionDesc(v).size();
        int nbProduits = produitRepository.findByVendeurIdOrderByNomAsc(id).size();
        return new int[]{nbLivraisons, nbTransactions, nbProduits};
    }

    public String vendeurNom(Long id) {
        return vendeurRepository.findById(id).map(User::getNomComplet).orElse("");
    }

    /**
     * Supprime le vendeur et toutes ses données, dans l'ordre imposé par les
     * contraintes de clés étrangères.
     *
     * @return [livraisonsSupprimees, transactionsSupprimees, produitsSupprimes]
     */
    @Transactional
    public int[] supprimerVendeur(Long id) {
        Vendeur v = vendeurRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé : " + id));

        // 1. Livraisons (leurs lignes partent en cascade ; mouvements déliés SET NULL).
        List<Livraison> livraisons = livraisonRepository.findByVendeur(v);
        int nbLivraisons = livraisons.size();
        livraisonRepository.deleteAll(livraisons);
        livraisonRepository.flush();

        // 2. Transactions.
        List<Transaction> transactions = transactionRepository.findByVendeurOrderByDateTransactionDesc(v);
        int nbTransactions = transactions.size();
        transactionRepository.deleteAll(transactions);
        transactionRepository.flush();

        // 3. Produits (leurs mouvements de stock partent en cascade côté base).
        List<Produit> produits = produitRepository.findByVendeurIdOrderByNomAsc(id);
        int nbProduits = produits.size();
        produitRepository.deleteAll(produits);
        produitRepository.flush();

        // 4. Le compte vendeur (la ligne users part en cascade).
        vendeurRepository.delete(v);

        log.warn("🗑️ Vendeur supprimé : {} (id={}) — {} livraison(s), {} transaction(s), {} produit(s)",
                v.getNomComplet(), id, nbLivraisons, nbTransactions, nbProduits);
        return new int[]{nbLivraisons, nbTransactions, nbProduits};
    }
}
