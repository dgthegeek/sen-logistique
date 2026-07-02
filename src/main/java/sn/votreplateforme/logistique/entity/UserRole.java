package sn.votreplateforme.logistique.entity;

public enum UserRole {
    VENDEUR,
    ADMIN,
    /**
     * Closeur / Assistante : appelle et confirme les commandes (module Closing).
     * Voit uniquement les commandes Nouvelle / A appeler / Confirmee.
     */
    CLOSEUR,
    /**
     * Livreur : exécute uniquement les livraisons qui lui sont assignées (module Dispatch).
     * Ne voit que "Mes livraisons".
     */
    LIVREUR,
    /**
     * Dispatcheur : prépare les commandes "Prête à livrer" à l'entrepôt et les assigne
     * à un livreur (module Dispatch). Ne voit que l'écran de dispatch.
     */
    DISPATCHEUR
}