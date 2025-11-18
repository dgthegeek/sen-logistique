package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Zone - Zones de livraison à Dakar
 *
 * Exemples :
 * - Zone 1 : Plateau, Ponty, Médina (tarif 1000 FCFA)
 * - Zone 2 : Sacré-Coeur, Mermoz (tarif 1500 FCFA)
 * - Zone 3 : Parcelles, Pikine (tarif 2000 FCFA)
 * - Zone 4 : Rufisque, Thiaroye (tarif 2500 FCFA)
 */
@Entity
@Table(name = "zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom de la zone (ex: "Zone 1", "Zone 2")
     */
    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    /**
     * Description (ex: "Plateau, Ponty, Médina")
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Tarif de livraison standard (NORMAL)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifStandard;

    /**
     * Tarif de livraison express (EXPRESS)
     * Généralement = tarifStandard * 1.5
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifExpress;

    /**
     * Zone active ou non
     */
    @Column(nullable = false)
    private boolean active = true;

    // ==================== RELATIONS ====================

    /**
     * Liste des quartiers dans cette zone
     * Relation OneToMany : Une zone a plusieurs quartiers
     */
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Quartier> quartiers = new ArrayList<>();

    /**
     * Liste des livraisons vers cette zone
     */
    @OneToMany(mappedBy = "adresseDestination.zone")
    @Builder.Default
    private List<Livraison> livraisons = new ArrayList<>();

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Ajoute un quartier à la zone
     */
    public void ajouterQuartier(Quartier quartier) {
        quartiers.add(quartier);
        quartier.setZone(this);
    }

    /**
     * Retire un quartier de la zone
     */
    public void retirerQuartier(Quartier quartier) {
        quartiers.remove(quartier);
        quartier.setZone(null);
    }

    /**
     * Calcule le tarif selon le type d'urgence
     */
    public BigDecimal getTarif(TypeUrgence urgence) {
        return urgence == TypeUrgence.EXPRESS ? tarifExpress : tarifStandard;
    }
}
