package sn.votreplateforme.logistique.enums;

public enum StatutVendeur {
    /**
     * Vendeur inscrit mais pas encore validé par un admin.
     * Le vendeur ne peut pas utiliser la plateforme.
     */
    EN_ATTENTE_VALIDATION,

    /**
     * Vendeur validé par un admin.
     * Le vendeur peut utiliser toutes les fonctionnalités de la plateforme.
     */
    ACTIF,

    /**
     * Vendeur temporairement désactivé.
     * Peut être réactivé par un admin.
     */
    SUSPENDU,

    /**
     * Vendeur définitivement bloqué (fraude, etc.).
     * Ne peut pas être réactivé.
     */
    BLOQUE
}