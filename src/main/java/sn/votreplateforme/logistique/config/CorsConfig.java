package sn.votreplateforme.logistique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ⚠️ Autoriser toutes les origines
        config.setAllowedOriginPatterns(List.of("*"));
        // ❌ Ne pas utiliser setAllowedOrigins("*") avec credentials

        // Autoriser toutes les méthodes HTTP
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Autoriser tous les headers
        config.setAllowedHeaders(List.of("*"));

        // Autoriser l'envoi de cookies / Authorization header
        config.setAllowCredentials(true);

        // Appliquer la config à toutes les routes
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
