package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité Livreur - Hérite de User
 *
 * Le livreur exécute uniquement les livraisons qui lui sont assignées par
 * l'administrateur (module Dispatch). Il ne voit que "Mes livraisons".
 */
@Entity
@Table(name = "livreurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livreur extends User {

    /**
     * Zone géographique habituelle du livreur (optionnel, indicatif).
     */
    @Column(length = 100)
    private String zonePreferee;

    /**
     * Livraisons actuellement assignées à ce livreur.
     */
    @OneToMany(mappedBy = "livreur")
    @Builder.Default
    private List<Livraison> livraisons = new ArrayList<>();
}
