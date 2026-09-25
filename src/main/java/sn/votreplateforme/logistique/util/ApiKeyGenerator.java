package sn.votreplateforme.logistique.util;

import java.security.SecureRandom;

/**
 * Génère les clés API des vendeurs (intégrations externes : Shopify, Zapier,
 * scripts maison...).
 *
 * <p>Contrairement à un numéro séquentiel, une clé générée avec
 * {@link SecureRandom} n'a besoin d'aucune coordination entre appels
 * concurrents : la probabilité de collision est astronomiquement nulle, la
 * contrainte unique en base n'est là que par prudence.
 */
public final class ApiKeyGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "dioks_live_";

    private ApiKeyGenerator() {
    }

    /** Génère une nouvelle clé API (32 octets aléatoires, encodés en hexadécimal). */
    public static String genererCle() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(PREFIX);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
