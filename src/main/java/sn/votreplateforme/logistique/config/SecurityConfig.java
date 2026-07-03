package sn.votreplateforme.logistique.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import sn.votreplateforme.logistique.security.JwtAuthenticationFilter;

/**
 * Configuration Spring Security
 * 
 * Configure :
 * - Les endpoints publics et protégés
 * - Le système d'authentification JWT
 * - Le chiffrement des mots de passe (BCrypt)
 * - Les filtres de sécurité
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * Configuration principale de la sécurité
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF (pas nécessaire avec JWT et API REST)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Désactiver CORS (on le gère dans CorsConfig)
            .cors(cors -> {})
            
            // Configuration des autorisations par endpoint
            .authorizeHttpRequests(authorize -> authorize
                
                // ==================== ENDPOINTS PUBLICS ====================
                
                // Authentication - Accessible sans connexion
                .requestMatchers("/auth/**", "/test/**").permitAll()
                
                // Tracking public - Accessible sans connexion
                .requestMatchers(HttpMethod.GET, "/tracking/**").permitAll()
                
                // Delivery (scan QR) - Accessible sans connexion
                .requestMatchers("/delivery/**").permitAll()
                
                // Zones et quartiers - Accessible sans connexion
                .requestMatchers(HttpMethod.GET, "/zones/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/quartiers/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/tarifs/calculer").permitAll()
                
                // Swagger UI - Accessible en développement
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3-docs/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                
                // Actuator - Accessible
                .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/api/actuator/**").permitAll()


                    // ==================== ENDPOINTS VENDEUR ====================
                .requestMatchers("/vendeur/livraisons/**").hasAnyRole("VENDEUR", "ADMIN")

                // Endpoints vendeur - Nécessite rôle VENDEUR
                .requestMatchers("/vendeur/**").hasRole("VENDEUR")

                // ==================== ENDPOINTS CLOSEUR ====================

                // Module Closing - Closeur (l'admin garde un droit de supervision)
                .requestMatchers("/closeur/**").hasAnyRole("CLOSEUR", "ADMIN")

                // ==================== ENDPOINTS LIVREUR ====================

                // Interface livreur - chaque livreur ne voit que ses livraisons
                .requestMatchers("/livreur/**").hasRole("LIVREUR")

                // ==================== ENDPOINTS DISPATCHEUR ====================

                // Module Dispatch - dispatcheur (l'admin garde un droit de supervision)
                .requestMatchers("/dispatch/**").hasAnyRole("DISPATCHEUR", "ADMIN")

                // Détail commande partagé (staff) : closeur, dispatcheur, livreur, admin
                .requestMatchers("/commandes/**").hasAnyRole("CLOSEUR", "DISPATCHEUR", "LIVREUR", "ADMIN")

                // ==================== ENDPOINTS ADMIN ====================

                // Endpoints admin - Nécessite rôle ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // ==================== AUTRES ====================
                
                // Toute autre requête nécessite une authentification
                .anyRequest().authenticated()
            )
            
            // Gestion des exceptions (401 Unauthorized)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"" + 
                        authException.getMessage() + "\"}"
                    );
                })
            )
            
            // Session stateless (pas de session côté serveur)
            // Tout est géré par le token JWT
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Provider d'authentification
            .authenticationProvider(authenticationProvider())
            
            // Ajouter le filtre JWT avant le filtre d'authentification standard
            .addFilterBefore(
                jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    /**
     * Provider d'authentification
     * 
     * Lie UserDetailsService et PasswordEncoder
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * AuthenticationManager - Nécessaire pour l'authentification
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * PasswordEncoder - BCrypt pour hasher les mots de passe
     * 
     * BCrypt est l'algorithme recommandé :
     * - Sécurisé
     * - Résistant aux attaques par force brute
     * - Inclut un salt automatiquement
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
