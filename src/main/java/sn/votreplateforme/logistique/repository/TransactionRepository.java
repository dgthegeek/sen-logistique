package sn.votreplateforme.logistique.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Transaction;
import sn.votreplateforme.logistique.entity.Transaction.StatutPaiement;
import sn.votreplateforme.logistique.entity.Transaction.TypeTransaction;
import sn.votreplateforme.logistique.entity.Vendeur;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Transaction
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // ==================== RECHERCHE PAR RÉFÉRENCE ====================
    
    /**
     * Trouve une transaction par sa référence
     * 
     * @param reference Référence unique (ex: "PAY-20251116-001")
     * @return Optional contenant la transaction si trouvée
     */
    Optional<Transaction> findByReference(String reference);
    
    /**
     * Vérifie si une référence existe déjà
     * 
     * @param reference Référence
     * @return true si la référence existe
     */
    boolean existsByReference(String reference);
    
    // ==================== RECHERCHE PAR VENDEUR ====================
    
    /**
     * Trouve toutes les transactions d'un vendeur
     * 
     * @param vendeur Vendeur
     * @return Liste des transactions
     */
    List<Transaction> findByVendeur(Vendeur vendeur);
    
    /**
     * Trouve les transactions d'un vendeur avec pagination
     * Utilisé dans l'historique des paiements du vendeur
     * 
     * @param vendeur Vendeur
     * @param pageable Pagination
     * @return Page de transactions
     */
    Page<Transaction> findByVendeur(Vendeur vendeur, Pageable pageable);
    
    /**
     * Trouve les transactions d'un vendeur par type
     * 
     * @param vendeur Vendeur
     * @param type Type de transaction
     * @return Liste des transactions
     */
    List<Transaction> findByVendeurAndType(Vendeur vendeur, TypeTransaction type);
    
    /**
     * Trouve les transactions d'un vendeur par statut
     * 
     * @param vendeur Vendeur
     * @param statut Statut du paiement
     * @return Liste des transactions
     */
    List<Transaction> findByVendeurAndStatut(Vendeur vendeur, StatutPaiement statut);
    
    // ==================== RECHERCHE PAR TYPE ====================
    
    /**
     * Trouve toutes les transactions d'un type donné
     * 
     * @param type Type de transaction
     * @return Liste des transactions
     */
    List<Transaction> findByType(TypeTransaction type);
    
    /**
     * Trouve les transactions par type avec pagination
     * 
     * @param type Type de transaction
     * @param pageable Pagination
     * @return Page de transactions
     */
    Page<Transaction> findByType(TypeTransaction type, Pageable pageable);
    
    // ==================== RECHERCHE PAR STATUT ====================
    
    /**
     * Trouve toutes les transactions avec un statut donné
     * 
     * @param statut Statut du paiement
     * @return Liste des transactions
     */
    List<Transaction> findByStatut(StatutPaiement statut);
    
    /**
     * Trouve les transactions en attente
     * Utilisé pour identifier les paiements à valider
     * 
     * @return Liste des transactions en attente
     */
    List<Transaction> findByStatutOrderByDateTransactionAsc(StatutPaiement statut);
    
    // ==================== RECHERCHE PAR DATE ====================
    
    /**
     * Trouve les transactions entre deux dates
     * Utilisé pour les rapports et statistiques
     * 
     * @param debut Date de début
     * @param fin Date de fin
     * @return Liste des transactions
     */
    List<Transaction> findByDateTransactionBetween(LocalDateTime debut, LocalDateTime fin);
    
    /**
     * Trouve les transactions d'aujourd'hui
     * 
     * @param debut Début du jour
     * @param fin Fin du jour
     * @return Liste des transactions du jour
     */
    @Query("SELECT t FROM Transaction t WHERE t.dateTransaction BETWEEN :debut AND :fin ORDER BY t.dateTransaction DESC")
    List<Transaction> findTransactionsDuJour(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
    
    // ==================== STATISTIQUES ====================
    
    /**
     * Calcule le montant total payé à un vendeur
     * 
     * @param vendeur Vendeur
     * @param statut Statut (normalement EFFECTUE)
     * @return Montant total payé
     */
    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.vendeur = :vendeur AND t.type = 'PAIEMENT_VENDEUR' AND t.statut = :statut")
    BigDecimal calculerMontantTotalPaye(@Param("vendeur") Vendeur vendeur, @Param("statut") StatutPaiement statut);
    
    /**
     * Calcule le montant total des commissions
     * 
     * @return Montant total des commissions
     */
    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.type = 'COMMISSION' AND t.statut = 'EFFECTUE'")
    BigDecimal calculerCommissionTotale();
    
    /**
     * Calcule le montant total des paiements en attente
     * 
     * @return Montant total en attente
     */
    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.type = 'PAIEMENT_VENDEUR' AND t.statut = 'EN_ATTENTE'")
    BigDecimal calculerMontantEnAttente();
    
    /**
     * Compte les transactions d'un vendeur dans une période
     * 
     * @param vendeur Vendeur
     * @param debut Date de début
     * @param fin Date de fin
     * @return Nombre de transactions
     */
    long countByVendeurAndDateTransactionBetween(Vendeur vendeur, LocalDateTime debut, LocalDateTime fin);
    
    // ==================== RECHERCHE AVANCÉE ====================
    
    /**
     * Recherche de transactions par plusieurs critères
     * Utilisé pour le filtrage dans le dashboard admin
     * 
     * @param vendeur Vendeur (optionnel)
     * @param type Type (optionnel)
     * @param statut Statut (optionnel)
     * @param debut Date de début (optionnel)
     * @param fin Date de fin (optionnel)
     * @param pageable Pagination
     * @return Page de transactions
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:vendeur IS NULL OR t.vendeur = :vendeur) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:statut IS NULL OR t.statut = :statut) AND " +
           "(:debut IS NULL OR t.dateTransaction >= :debut) AND " +
           "(:fin IS NULL OR t.dateTransaction <= :fin) " +
           "ORDER BY t.dateTransaction DESC")
    Page<Transaction> rechercherTransactions(
        @Param("vendeur") Vendeur vendeur,
        @Param("type") TypeTransaction type,
        @Param("statut") StatutPaiement statut,
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin,
        Pageable pageable
    );
    
    /**
     * Trouve les transactions par vendeur et période
     */
    Page<Transaction> findByVendeurAndDateTransactionBetween(
            Vendeur vendeur,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable
    );

    /**
     * Trouve les transactions par période
     */
    Page<Transaction> findByDateTransactionBetween(
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable
    );

    /**
     * Compte les transactions dans une période
     */
    long countByDateTransactionBetween(
            LocalDateTime dateDebut,
            LocalDateTime dateFin
    );
}
