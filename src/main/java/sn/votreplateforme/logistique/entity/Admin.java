package sn.votreplateforme.logistique.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Entité Admin - Hérite de User
 * 
 * Représente un administrateur qui gère :
 * - Les ramassages groupés
 * - Les livraisons
 * - Les paiements aux vendeurs
 * - Les finances
 * 
 * Pour le MVP, l'admin fait aussi le rôle de livreur
 */
@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends User {
    
    // Pour le MVP, Admin n'a pas de champs spécifiques
    // Tous les champs nécessaires sont dans User
    
    // Dans une version future, on pourrait ajouter :
    // - Zone géographique assignée
    // - Permissions spécifiques
    // - Statistiques de performance
    // etc.
}
