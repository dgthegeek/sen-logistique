package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entité Quartier - Quartiers de Dakar
 *
 * Chaque quartier appartient à une Zone
 * Utilisé pour l'auto-complétion dans le formulaire de création de livraison
 */
@Entity
@Table(name = "quartiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quartier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom du quartier (ex: "Mermoz", "Sacré-Coeur", "Plateau")
     */
    @Column(nullable = false, length = 100)
    private String nom;

    /**
     * Commune (ex: "Dakar", "Pikine", "Guédiawaye")
     */
    @Column(nullable = false, length = 100)
    private String commune;

    /**
     * Quartier actif ou non
     */
    @Column(nullable = false)
    private boolean actif = true;

    // ==================== RELATIONS ====================

    /**
     * Zone à laquelle appartient ce quartier
     * Relation ManyToOne : Plusieurs quartiers appartiennent à une zone
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    public void setZone(Zone zone) {
        this.zone = zone;
    }
}
