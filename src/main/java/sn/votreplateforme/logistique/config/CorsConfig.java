package sn.votreplateforme.logistique.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration CORS (Cross-Origin Resource Sharing)
 * 
 * Permet au frontend Angular (sur un autre port/domaine) 
 * d'appeler l'API backend
 */
@Configuration
public class CorsConfig {
    
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;
    
    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;
    
    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;
    
    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;
    
    @Value("${app.cors.max-age}")
    private long maxAge;
    
    /**
     * Configuration CORS globale
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origines autorisées (ex: http://localhost:4200)
        configuration.setAllowedOrigins(parseList(allowedOrigins));
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(parseList(allowedMethods));
        
        // Headers autorisés
        configuration.setAllowedHeaders(parseList(allowedHeaders));
        
        // Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"
        ));
        
        // Autoriser les credentials (cookies, authorization headers)
        configuration.setAllowCredentials(allowCredentials);
        
        // Durée de cache de la configuration CORS (en secondes)
        configuration.setMaxAge(maxAge);
        
        // Appliquer cette configuration à tous les endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Parse une chaîne séparée par des virgules en liste
     * 
     * @param value Chaîne à parser (ex: "GET,POST,PUT")
     * @return Liste de valeurs
     */
    private List<String> parseList(String value) {
        if (value == null || value.isEmpty()) {
            return Arrays.asList("*");
        }
        return Arrays.asList(value.split(","));
    }
}
