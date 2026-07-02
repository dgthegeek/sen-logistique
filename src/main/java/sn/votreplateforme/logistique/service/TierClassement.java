package sn.votreplateforme.logistique.service;

/**
 * Paliers (tiers) de la Dioks League, débloqués selon le nombre de livraisons livrées.
 * Ordre croissant : chaque tier a un seuil minimum de livraisons.
 */
public enum TierClassement {
    BRONZE(0),
    ARGENT(15),
    OR(40),
    PLATINE(100),
    DIAMANT(250),
    LEGENDE(500);

    private final int seuil;

    TierClassement(int seuil) {
        this.seuil = seuil;
    }

    public int getSeuil() {
        return seuil;
    }

    /** Tier atteint pour un nombre de livraisons donné. */
    public static TierClassement forLivraisons(long nombreLivraisons) {
        TierClassement resultat = BRONZE;
        for (TierClassement t : values()) {
            if (nombreLivraisons >= t.seuil) {
                resultat = t;
            }
        }
        return resultat;
    }

    /** Tier suivant, ou null si déjà au sommet (Légende). */
    public TierClassement suivant() {
        TierClassement[] all = values();
        int idx = ordinal();
        return idx < all.length - 1 ? all[idx + 1] : null;
    }
}
