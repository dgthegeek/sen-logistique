package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entité MouvementStock - Journal de tous les mouvements de stock d'un produit.
 *
 * Permet l'inventaire (entrées/sorties) et la traçabilité.
 */
@Entity
@Table(name = "mouvements_stock")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeMouvement type;

    /**
     * Variation appliquée : positive (entrée) ou négative (sortie).
     */
    @Column(nullable = false)
    private Integer variation;

    @Column(name = "stock_avant", nullable = false)
    private Integer stockAvant;

    @Column(name = "stock_apres", nullable = false)
    private Integer stockApres;

    /**
     * Référence à la livraison à l'origine d'une SORTIE (optionnel).
     */
    @Column(name = "livraison_id")
    private Long livraisonId;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    /**
     * Auteur de l'opération (nom de l'admin ou "Système").
     */
    @Column(length = 100)
    private String auteur;

    @CreatedDate
    @Column(name = "date_mouvement", nullable = false, updatable = false)
    private LocalDateTime dateMouvement;
}
