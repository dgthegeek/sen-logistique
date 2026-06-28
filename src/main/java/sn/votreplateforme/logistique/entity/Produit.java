package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Produit - Marchandise d'un partenaire (vendeur) stockée au warehouse.
 *
 * Chaque produit a un identifiant unique (ex: DKS-00001) et un QR code.
 * Le stock est suivi via quantiteStock et historisé dans MouvementStock.
 */
@Entity
@Table(name = "produits")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Code unique du produit (ex: "DKS-00001"). Encodé dans le QR code.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Partenaire (vendeur) propriétaire de la marchandise.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Vendeur vendeur;

    /**
     * Prix unitaire indicatif (optionnel).
     */
    @Column(name = "prix_unitaire", precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    /**
     * Quantité actuellement en stock.
     */
    @Column(name = "quantite_stock", nullable = false)
    private Integer quantiteStock = 0;

    /**
     * Seuil d'alerte de rupture : en dessous (ou égal), le produit est "stock faible".
     */
    @Column(name = "seuil_alerte", nullable = false)
    private Integer seuilAlerte = 5;

    /**
     * URL encodée dans le QR code du produit.
     */
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    /**
     * Produit actif (commercialisable) ou non.
     */
    @Column(nullable = false)
    private boolean actif = true;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // ==================== MÉTHODES UTILITAIRES ====================

    public void ajouterStock(int quantite) {
        this.quantiteStock += quantite;
    }

    public void retirerStock(int quantite) {
        this.quantiteStock = Math.max(0, this.quantiteStock - quantite);
    }

    /** Vrai si le stock est en rupture ou faible (<= seuil). */
    public boolean enAlerte() {
        return quantiteStock <= seuilAlerte;
    }
}
