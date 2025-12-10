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
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.entity.UserRole;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.repository.UserRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.JwtTokenProvider;

import java.math.BigDecimal;

/**
 * Service d'authentification
 * Gère l'inscription des vendeurs et la connexion (login) pour vendeurs ET admins
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final VendeurRepository vendeurRepository;
    private final UserRepository userRepository;
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
        if (userRepository.findByTelephone(request.getTelephone()).isPresent()) {
            throw new IllegalArgumentException("Un compte existe déjà avec ce numéro de téléphone");
        }

        // 2. Vérifier que l'email n'existe pas (si fourni)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
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

        // 5. Authentifier le vendeur via Spring Security (comme pour le login)
        // Cela charge les UserDetails via UserDetailsService et crée une Authentication valide
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getTelephone(),
                        request.getPassword() // On utilise le mot de passe en clair (avant hash)
                )
        );

        // 6. Mettre l'utilisateur dans le SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 7. Générer le token JWT
        String token = jwtTokenProvider.generateToken(authentication);

        // 8. Créer la réponse
        return buildAuthResponse(vendeur, token);
    }

    /**
     * Connexion (login)
     * Fonctionne pour les VENDEURS et les ADMINS
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

        // 3. Récupérer l'utilisateur (vendeur OU admin)
        // CORRECTION : Utiliser UserRepository au lieu de VendeurRepository
        User user = userRepository.findByTelephone(request.getTelephone())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        // 4. Vérifier que le compte est actif
        if (!user.isActif()) {
            throw new IllegalArgumentException("Ce compte a été désactivé. Contactez l'administrateur.");
        }

        log.info("Connexion réussie pour: {} (ID: {}, Rôle: {})",
                request.getTelephone(), user.getId(), user.getRole());

        // 5. Générer le token JWT avec l'objet Authentication
        String token = jwtTokenProvider.generateToken(authentication);

        // 6. Créer la réponse
        return buildAuthResponse(user, token);
    }

    /**
     * Construire la réponse d'authentification
     * Fonctionne pour les vendeurs ET les admins
     */
    private AuthResponse buildAuthResponse(User user, String token) {
        // Créer UserInfo
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setNom(user.getNom());
        userInfo.setPrenom(user.getPrenom());
        userInfo.setTelephone(user.getTelephone());
        userInfo.setEmail(user.getEmail());

        // Convertir l'Entity UserRole en DTO UserRole
        userInfo.setRole(sn.votreplateforme.logistique.dto.UserRole.valueOf(user.getRole().name()));

        // Si c'est un vendeur, ajouter le nom de boutique
        if (user instanceof Vendeur) {
            Vendeur vendeur = (Vendeur) user;
            userInfo.setNomBoutique(vendeur.getNomBoutique());
            userInfo.setStatut(StatutVendeur.fromValue(vendeur.getStatut().name()));
        }

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