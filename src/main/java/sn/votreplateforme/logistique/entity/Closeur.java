package sn.votreplateforme.logistique.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entité Closeur - Hérite de User
 *
 * Le closeur (ou assistante) appelle les clients pour confirmer les commandes
 * dans le module Closing. Il ne voit que les commandes Nouvelle / A appeler / Confirmee.
 */
@Entity
@Table(name = "closeurs")
@Getter
@Setter
@NoArgsConstructor
public class Closeur extends User {

    /**
     * Vendeurs auxquels ce closeur est restreint : il ne voit et ne peut
     * fermer que les commandes de ces vendeurs. Vide = pas de restriction
     * (il peut fermer les commandes de n'importe quel vendeur).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "closeur_vendeurs",
            joinColumns = @JoinColumn(name = "closeur_id"),
            inverseJoinColumns = @JoinColumn(name = "vendeur_id")
    )
    private Set<Vendeur> vendeursAssignes = new HashSet<>();
}
