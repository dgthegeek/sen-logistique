package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Versement d'un livreur vers le coordinateur logistique / l'admin.
 *
 * <p>Représente une remise de cash (COD) : lorsqu'un livreur reverse l'argent
 * collecté sur ses livraisons, un {@code VersementLivreur} est créé pour garder
 * la trace horodatée de l'opération (montant, nombre de livraisons couvertes,
 * qui a validé). Toutes les livraisons concernées sont alors marquées comme
 * réglées et rattachées à ce versement.
 */
@Entity
@Table(name = "versements_livreur")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersementLivreur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Livreur qui a reversé le cash. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livreur_id", nullable = false)
    private Livreur livreur;

    /** Montant total reversé (Σ cash collecté des livraisons couvertes). */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    /** Nombre de livraisons couvertes par ce versement. */
    @Column(name = "nombre_livraisons", nullable = false)
    private Integer nombreLivraisons;

    /** Date du versement (auto-générée). */
    @CreatedDate
    @Column(name = "date_versement", nullable = false, updatable = false)
    private LocalDateTime dateVersement;

    /** Nom de la personne (coordinateur/admin) qui a validé le versement. */
    @Column(name = "effectue_par", length = 150)
    private String effectuePar;

    /** Rôle de la personne qui a validé (ADMIN / DISPATCHEUR). */
    @Column(name = "effectue_par_role", length = 30)
    private String effectueParRole;

    /** Commentaire libre (optionnel). */
    @Column(columnDefinition = "TEXT")
    private String commentaire;
}
