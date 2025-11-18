package sn.votreplateforme.logistique.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utilitaires pour la sécurité
 * 
 * Fournit des méthodes statiques pour récupérer l'utilisateur connecté
 * depuis n'importe où dans l'application
 */
public class SecurityUtils {
    
    /**
     * Récupère l'Authentication du SecurityContext
     * 
     * @return Authentication ou null si non authentifié
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    /**
     * Récupère le téléphone (username) de l'utilisateur connecté
     * 
     * Utilisé dans les services pour identifier l'utilisateur qui fait la requête
     * 
     * @return Téléphone de l'utilisateur connecté ou null
     */
    public static String getCurrentUserTelephone() {
        Authentication authentication = getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        return null;
    }
    
    /**
     * Récupère les UserDetails de l'utilisateur connecté
     * 
     * @return UserDetails ou null
     */
    public static UserDetails getCurrentUser() {
        Authentication authentication = getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        }
        
        return null;
    }
    
    /**
     * Vérifie si l'utilisateur est authentifié
     * 
     * @return true si authentifié
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null 
            && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * Vérifie si l'utilisateur a un rôle spécifique
     * 
     * @param role Rôle à vérifier (ex: "ROLE_ADMIN", "ROLE_VENDEUR")
     * @return true si l'utilisateur a le rôle
     */
    public static boolean hasRole(String role) {
        Authentication authentication = getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
    
    /**
     * Vérifie si l'utilisateur est ADMIN
     * 
     * @return true si ADMIN
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
    
    /**
     * Vérifie si l'utilisateur est VENDEUR
     * 
     * @return true si VENDEUR
     */
    public static boolean isVendeur() {
        return hasRole("ROLE_VENDEUR");
    }
}
