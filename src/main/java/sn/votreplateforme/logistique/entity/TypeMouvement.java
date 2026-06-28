package sn.votreplateforme.logistique.entity;

/**
 * Type de mouvement de stock.
 */
public enum TypeMouvement {
    /** Entrée de stock (réception de marchandise). */
    ENTREE,
    /** Sortie de stock (livraison effectuée). */
    SORTIE,
    /** Ajustement manuel suite à un inventaire physique. */
    AJUSTEMENT,
    /** Création initiale du produit. */
    CREATION
}
