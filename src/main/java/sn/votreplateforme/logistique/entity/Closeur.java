package sn.votreplateforme.logistique.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Entité Closeur - Hérite de User
 *
 * Le closeur (ou assistante) appelle les clients pour confirmer les commandes
 * dans le module Closing. Il ne voit que les commandes Nouvelle / A appeler / Confirmee.
 *
 * Comme Admin, le closeur n'a pas de champ spécifique pour l'instant : tous les
 * champs nécessaires sont dans User.
 */
@Entity
@Table(name = "closeurs")
@Getter
@Setter
@NoArgsConstructor
public class Closeur extends User {
}
