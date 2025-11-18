package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AuthenticationApi;
import sn.votreplateforme.logistique.dto.AuthForgotPasswordPost200Response;
import sn.votreplateforme.logistique.dto.AuthRefreshPostRequest;
import sn.votreplateforme.logistique.dto.AuthResponse;
import sn.votreplateforme.logistique.dto.LoginRequest;
import sn.votreplateforme.logistique.dto.RegisterRequest;
import sn.votreplateforme.logistique.service.AuthService;

/**
 * Controller d'authentification
 * Implémente l'interface AuthenticationApi générée automatiquement par OpenAPI
 * 
 * Endpoints:
 * - POST /auth/register - Inscription d'un nouveau vendeur
 * - POST /auth/login - Connexion
 * - POST /auth/refresh - Rafraîchir le token JWT
 * - POST /auth/forgot-password - Réinitialisation mot de passe
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {
    
    private final AuthService authService;
    
    /**
     * POST /auth/register
     * Inscription d'un nouveau vendeur
     */
    @Override
    public ResponseEntity<AuthResponse> authRegisterPost(RegisterRequest registerRequest) {
        log.info("=== Requête d'inscription reçue ===");
        log.debug("Téléphone: {}", registerRequest.getTelephone());
        
        try {
            AuthResponse response = authService.register(registerRequest);
            
            log.info("✅ Inscription réussie pour: {}", registerRequest.getTelephone());
            
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            log.warn("❌ Erreur d'inscription: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de l'inscription", e);
            throw new RuntimeException("Erreur lors de l'inscription: " + e.getMessage());
        }
    }

    /**
     * POST /auth/login
     * Connexion
     */
    @Override
    public ResponseEntity<AuthResponse> authLoginPost(LoginRequest loginRequest) {
        log.info("=== Requête de connexion reçue ===");
        log.debug("Téléphone: {}", loginRequest.getTelephone());
        
        try {
            AuthResponse response = authService.login(loginRequest);
            
            log.info("✅ Connexion réussie pour: {}", loginRequest.getTelephone());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ Échec de connexion: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la connexion", e);
            throw new RuntimeException("Erreur lors de la connexion: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<AuthForgotPasswordPost200Response> authForgotPasswordPost(sn.votreplateforme.logistique.dto.AuthForgotPasswordPostRequest authForgotPasswordPostRequest) {
        return null;
    }

    @Override
    public ResponseEntity<AuthResponse> authRefreshPost(AuthRefreshPostRequest authRefreshPostRequest) {
        return null;
    }
}
