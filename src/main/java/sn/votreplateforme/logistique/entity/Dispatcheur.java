package sn.votreplateforme.logistique.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Entité Dispatcheur - Hérite de User
 *
 * Le dispatcheur prépare à l'entrepôt les commandes rendues "Prête à livrer" par le
 * closeur, puis les assigne à un livreur (module Dispatch). Il ne voit que l'écran
 * de dispatch. Comme le closeur, il n'a pas de champ spécifique : tout est dans User.
 */
@Entity
@Table(name = "dispatcheurs")
@Getter
@Setter
@NoArgsConstructor
public class Dispatcheur extends User {
}
