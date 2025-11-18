package sn.votreplateforme.logistique.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.repository.UserRepository;

import java.util.Collection;
import java.util.Collections;

/**
 * Service de chargement des utilisateurs pour Spring Security
 * 
 * Implémente UserDetailsService qui est l'interface standard de Spring Security
 * pour charger les utilisateurs depuis une source de données
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Charge un utilisateur par son téléphone (username)
     * 
     * Appelé automatiquement par Spring Security lors de l'authentification
     * 
     * @param telephone Numéro de téléphone (username)
     * @return UserDetails contenant les infos de l'utilisateur
     * @throws UsernameNotFoundException Si l'utilisateur n'existe pas
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String telephone) throws UsernameNotFoundException {
        log.debug("Chargement de l'utilisateur avec téléphone : {}", telephone);
        
        // 1. Chercher l'utilisateur dans la base de données
        User user = userRepository.findByTelephone(telephone)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "Utilisateur non trouvé avec le téléphone : " + telephone
                ));
        
        // 2. Vérifier que le compte est actif
        if (!user.isActif()) {
            throw new UsernameNotFoundException("Le compte est désactivé : " + telephone);
        }
        
        // 3. Créer l'objet UserDetails de Spring Security
        return buildUserDetails(user);
    }
    
    /**
     * Construit l'objet UserDetails à partir de notre entité User
     * 
     * @param user Notre entité User
     * @return UserDetails pour Spring Security
     */
    private UserDetails buildUserDetails(User user) {
        // Créer les autorités (rôles) de l'utilisateur
        Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
        
        // Retourner un UserDetails avec :
        // - username = téléphone
        // - password = mot de passe hashé
        // - authorities = rôles
        // - flags d'état du compte
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getTelephone())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActif())
                .build();
    }
    
    /**
     * Crée les autorités (rôles) de l'utilisateur
     * 
     * Spring Security utilise le préfixe "ROLE_" par convention
     * 
     * @param user Notre entité User
     * @return Collection d'autorités
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // Convertir notre enum UserRole en GrantedAuthority de Spring Security
        // VENDEUR → ROLE_VENDEUR
        // ADMIN → ROLE_ADMIN
        String roleName = "ROLE_" + user.getRole().name();
        
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }
}
