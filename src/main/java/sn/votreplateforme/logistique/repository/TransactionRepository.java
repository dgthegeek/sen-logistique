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

/**
 * Repository pour l'entité Transaction
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ==================== RECHERCHES PAR VENDEUR ====================

    Page<Transaction> findByVendeur(Vendeur vendeur, Pageable pageable);

    List<Transaction> findByVendeurOrderByDateTransactionDesc(Vendeur vendeur);

    // ==================== RECHERCHES PAR TYPE ET STATUT ====================

    List<Transaction> findByVendeurAndTypeAndStatut(
            Vendeur vendeur,
            TypeTransaction type,
            StatutPaiement statut
    );

    boolean existsByVendeurAndTypeAndStatut(
            Vendeur vendeur,
            TypeTransaction type,
            StatutPaiement statut
    );

    // ==================== RECHERCHES PAR DATES ====================

    Page<Transaction> findByVendeurAndDateTransactionBetween(
            Vendeur vendeur,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable
    );

    Page<Transaction> findByDateTransactionBetween(
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable
    );

    long countByDateTransactionBetween(
            LocalDateTime dateDebut,
            LocalDateTime dateFin
    );

    // ==================== CALCULS FINANCIERS ====================

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.type = 'COMMISSION' AND t.statut = 'EFFECTUE'")
    BigDecimal calculerCommissionTotale();

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.type = 'PAIEMENT_VENDEUR' AND t.statut = 'EN_ATTENTE'")
    BigDecimal calculerMontantEnAttente();

    /**
     * Total effectivement payé à un vendeur (paiements EFFECTUE uniquement).
     * Sert à calculer dynamiquement le solde disponible = CA livré - total payé.
     */
    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.vendeur = :vendeur AND t.type = 'PAIEMENT_VENDEUR' AND t.statut = 'EFFECTUE'")
    BigDecimal sumPaiementsEffectues(@Param("vendeur") Vendeur vendeur);

    /** Première (plus ancienne) demande de paiement d'un vendeur dans un statut donné. */
    java.util.Optional<Transaction> findFirstByVendeurAndTypeAndStatutOrderByDateTransactionAsc(
            Vendeur vendeur,
            TypeTransaction type,
            StatutPaiement statut
    );

    // ==================== RECHERCHES AVANCÉES ====================

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
}