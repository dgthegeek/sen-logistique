package sn.votreplateforme.logistique.entity;

public enum StatutLivraison {

    // ==================== NOUVEAU CYCLE (Closing + Dispatch) ====================
    /** Commande reçue, en attente de prise en charge par le closeur. */
    NOUVELLE,
    /** Le closeur doit appeler le client. */
    A_APPELER,
    /** Le client a confirmé la commande. */
    CONFIRMEE,
    /** Commande confirmée et prête à être assignée à un livreur. */
    PRETE_A_LIVRER,
    /** Commande assignée à un livreur précis (Dispatch). */
    ASSIGNEE,
    /** Le livreur a démarré la tournée. */
    EN_LIVRAISON,
    /** Commande livrée avec succès. */
    LIVREE,
    /** Échec de livraison (le motif est précisé dans MotifEchec). */
    ECHEC,
    /** Commande annulée. */
    ANNULEE,

    // ==================== ANCIEN CYCLE (module ramassage - dormant) ====================
    // Conservés pour compatibilité avec le module ramassage et l'historique existant.
    EN_ATTENTE_RAMASSAGE,
    RAMASSE,
    EN_ROUTE,
    ECHEC_ABSENT,
    ECHEC_REFUSE
}