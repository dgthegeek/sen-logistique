package sn.votreplateforme.logistique.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT - Intercepte toutes les requêtes HTTP
 * 
 * Rôle :
 * 1. Extraire le token JWT du header "Authorization"
 * 2. Valider le token
 * 3. Charger l'utilisateur correspondant
 * 4. Mettre l'utilisateur dans le SecurityContext de Spring
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    /**
     * Filtre principal - Appelé pour chaque requête HTTP
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // 1. Extraire le token JWT du header
            String jwt = getJwtFromRequest(request);
            
            // 2. Si token présent et valide
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                
                // 3. Extraire le téléphone (username) du token
                String telephone = tokenProvider.getTelephoneFromToken(jwt);
                
                // 4. Charger l'utilisateur depuis la base de données
                UserDetails userDetails = userDetailsService.loadUserByUsername(telephone);
                
                // 5. Créer l'objet Authentication
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // 6. Mettre l'utilisateur authentifié dans le SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Utilisateur authentifié : {}", telephone);
            }
            
        } catch (Exception ex) {
            log.error("Impossible d'authentifier l'utilisateur", ex);
        }
        
        // 7. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extrait le token JWT du header Authorization
     * 
     * Format attendu : "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * 
     * @param request Requête HTTP
     * @return Token JWT (sans le préfixe "Bearer ") ou null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Vérifier que le header existe et commence par "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Retourner le token sans le préfixe "Bearer "
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
