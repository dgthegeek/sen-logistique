package sn.votreplateforme.logistique.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

/**
 * Adresse embeddable - Réutilisée dans Vendeur et Livraison
 *
 * @Embeddable = Pas une table séparée, mais des colonnes incluses dans la table parent
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adresse {

    /**
     * Commune (ex: "Dakar", "Pikine", "Guédiawaye")
     */
    private String commune;

    /**
     * Quartier (ex: "Mermoz", "Sacré-Coeur", "Plateau")
     */
    private String quartier;

    /**
     * Adresse complète (ex: "Cité Biagui, Villa 45")
     */
    private String adresseComplete;

    /**
     * Point de repère (ex: "Face à la mosquée", "Près de la station Total")
     */
    private String pointRepere;

    /**
     * Zone de livraison associée
     * Utilisé pour calculer le tarif
     */
    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;
}
