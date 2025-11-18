package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.AuthResponse;
import sn.votreplateforme.logistique.dto.LoginRequest;
import sn.votreplateforme.logistique.dto.RegisterRequest;
import sn.votreplateforme.logistique.dto.UserInfo;
import sn.votreplateforme.logistique.entity.UserRole;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.JwtTokenProvider;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * Service d'authentification
 * Gère l'inscription des vendeurs et la connexion (login)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final VendeurRepository vendeurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Inscription d'un nouveau vendeur
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Tentative d'inscription pour le téléphone: {}", request.getTelephone());

        // 1. Vérifier que le téléphone n'existe pas déjà
        if (vendeurRepository.findByTelephone(request.getTelephone()).isPresent()) {
            throw new IllegalArgumentException("Un compte existe déjà avec ce numéro de téléphone");
        }

        // 2. Vérifier que l'email n'existe pas (si fourni)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (vendeurRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Un compte existe déjà avec cet email");
            }
        }

        // 3. Créer le nouveau vendeur
        Vendeur vendeur = new Vendeur();
        vendeur.setNom(request.getNom());
        vendeur.setPrenom(request.getPrenom());
        vendeur.setTelephone(request.getTelephone());
        vendeur.setEmail(request.getEmail());
        vendeur.setPassword(passwordEncoder.encode(request.getPassword())); // Hash BCrypt
        vendeur.setRole(UserRole.VENDEUR);
        vendeur.setActif(true);

        // Informations boutique
        vendeur.setNomBoutique(request.getNomBoutique());
        vendeur.setCategorieActivite(request.getCategorieActivite());
        vendeur.setInstagram(request.getInstagram());
        vendeur.setFacebook(request.getFacebook());

        // Adresse de ramassage
        vendeur.setCommune(request.getCommune());
        vendeur.setQuartier(request.getQuartier());
        vendeur.setAdresseComplete(request.getAdresseComplete());

        // Finances
        vendeur.setSoldeEnAttente(BigDecimal.ZERO);

        // 4. Sauvegarder
        vendeur = vendeurRepository.save(vendeur);

        log.info("Nouveau vendeur inscrit avec succès: {} (ID: {})", request.getTelephone(), vendeur.getId());

        // 5. Créer une Authentication pour générer le token JWT
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                vendeur.getTelephone(),
                null, // Le password n'est pas nécessaire ici
                Collections.emptyList() // Pas besoin des authorities pour juste générer le token
        );

        // 6. Générer le token JWT
        String token = jwtTokenProvider.generateToken(authentication);

        // 7. Créer la réponse
        return buildAuthResponse(vendeur, token);
    }

    /**
     * Connexion (login)
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour le téléphone: {}", request.getTelephone());

        // 1. Authentifier via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getTelephone(),
                        request.getPassword()
                )
        );

        // 2. Mettre l'utilisateur dans le SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Récupérer le vendeur
        Vendeur vendeur = vendeurRepository.findByTelephone(request.getTelephone())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        // 4. Vérifier que le compte est actif
        if (!vendeur.isActif()) {
            throw new IllegalArgumentException("Ce compte a été désactivé. Contactez l'administrateur.");
        }

        log.info("Connexion réussie pour: {} (ID: {})", request.getTelephone(), vendeur.getId());

        // 5. Générer le token JWT avec l'objet Authentication
        String token = jwtTokenProvider.generateToken(authentication);

        // 6. Créer la réponse
        return buildAuthResponse(vendeur, token);
    }

    /**
     * Construire la réponse d'authentification
     */
    private AuthResponse buildAuthResponse(Vendeur vendeur, String token) {
        // Créer UserInfo
        UserInfo userInfo = new UserInfo();
        userInfo.setId(vendeur.getId());
        userInfo.setNom(vendeur.getNom());
        userInfo.setPrenom(vendeur.getPrenom());
        userInfo.setTelephone(vendeur.getTelephone());
        userInfo.setEmail(vendeur.getEmail());
        userInfo.setRole(sn.votreplateforme.logistique.dto.UserRole.VENDEUR);
        userInfo.setNomBoutique(vendeur.getNomBoutique());

        // Créer AuthResponse
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setType("Bearer");
        response.setUser(userInfo);

        // Note: refreshToken sera implémenté plus tard si nécessaire

        return response;
    }

    /**
     * Rafraîchir le token JWT
     * (À implémenter plus tard si nécessaire)
     */
    public AuthResponse refreshToken(String refreshToken) {
        // TODO: Implémenter la logique de refresh token
        throw new UnsupportedOperationException("Refresh token pas encore implémenté");
    }
}