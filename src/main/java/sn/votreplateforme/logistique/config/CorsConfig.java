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
        CorsConfiguration configuration = new CorsConfiguration();

        // Autoriser toutes les origines
        // Si tu utilises Spring Boot 2.4+ / Spring Framework 5.3+,
        // et allowCredentials=true, il vaut mieux utiliser allowedOriginPatterns
        configuration.setAllowedOriginPatterns(List.of("*"));
        // Ou (si tu tiens à utiliser allowedOrigins) :
        // configuration.setAllowedOrigins(List.of("*"));

        // Autoriser toutes les méthodes
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Autoriser tous les headers
        configuration.setAllowedHeaders(List.of("*"));

        // Autoriser l’envoi de credentials (cookies, Authorization header…)
        configuration.setAllowCredentials(true);

        // Exposer les headers souhaités (si nécessaire côté client)
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Total-Count"));

        // Durée de mise en cache de la config CORS (en secondes)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
