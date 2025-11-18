package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Transaction - Historique des paiements aux vendeurs
 * 
 * Enregistre chaque paiement effectué par l'admin aux vendeurs
 */
@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Vendeur qui reçoit le paiement
     * Relation ManyToOne : Plusieurs transactions pour un vendeur
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Vendeur vendeur;
    
    /**
     * Montant du paiement en FCFA
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;
    
    /**
     * Type de transaction
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeTransaction type;
    
    /**
     * Référence unique de la transaction (ex: "PAY-20251116-001")
     */
    @Column(nullable = false, unique = true, length = 100)
    private String reference;
    
    /**
     * Statut du paiement
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPaiement statut;
    
    /**
     * Commentaire ou notes (ex: "Paiement en cash remis au vendeur")
     */
    @Column(columnDefinition = "TEXT")
    private String commentaire;
    
    /**
     * Date de la transaction (auto-générée)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateTransaction;
    
    /**
     * Admin qui a effectué le paiement
     */
    @Column(length = 100)
    private String adminNom;
    
    // ==================== ENUMS INTERNES ====================
    
    /**
     * Types de transaction possibles
     */
    public enum TypeTransaction {
        /**
         * Paiement au vendeur (cash remis)
         */
        PAIEMENT_VENDEUR,
        
        /**
         * Collection de cash (COD collecté lors d'une livraison)
         */
        COLLECTE_COD,
        
        /**
         * Commission de la plateforme
         */
        COMMISSION
    }
    
    /**
     * Statuts de paiement possibles
     */
    public enum StatutPaiement {
        /**
         * Paiement effectué avec succès
         */
        EFFECTUE,
        
        /**
         * Paiement en attente de validation
         */
        EN_ATTENTE,
        
        /**
         * Paiement annulé
         */
        ANNULE
    }
}
