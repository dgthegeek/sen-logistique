package sn.votreplateforme.logistique.entity;

/**
 * Motif d'échec d'une livraison (module Dispatch / Closing).
 * Obligatoire lorsqu'une livraison passe au statut ECHEC.
 */
public enum MotifEchec {
    TELEPHONE_INJOIGNABLE,
    CLIENT_ABSENT,
    ADRESSE_INCORRECTE,
    REFUS_CLIENT,
    REPORT_CLIENT
}
