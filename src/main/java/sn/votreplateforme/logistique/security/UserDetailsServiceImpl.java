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
 * Service qui charge les utilisateurs depuis la base de données pour Spring Security
 * Fonctionne pour les VENDEURS et les ADMINS
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur par son téléphone (username dans notre cas)
     * Cherche dans la table users (parent) donc fonctionne pour vendeurs ET admins
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String telephone) throws UsernameNotFoundException {
        log.debug("Chargement de l'utilisateur: {}", telephone);

        // ✅ CORRECTION : Chercher dans UserRepository au lieu de VendeurRepository
        // Cela permet de charger les vendeurs ET les admins
        User user = userRepository.findByTelephone(telephone)
                .orElseThrow(() -> {
                    log.error("Utilisateur non trouvé: {}", telephone);
                    return new UsernameNotFoundException("Utilisateur non trouvé avec le téléphone: " + telephone);
                });

        // Vérifier que le compte est actif
        if (!user.isActif()) {
            log.error("Compte désactivé: {}", telephone);
            throw new UsernameNotFoundException("Ce compte a été désactivé");
        }

        log.debug("Utilisateur trouvé: {} {} - Rôle: {}",
                user.getPrenom(), user.getNom(), user.getRole());

        // Créer et retourner le UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getTelephone())
                .password(user.getPassword())
                .authorities(getAuthorities(user))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActif())
                .build();
    }

    /**
     * Construit les autorités (rôles) de l'utilisateur
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // Spring Security utilise le préfixe "ROLE_" par convention
        String authority = "ROLE_" + user.getRole().name();

        log.debug("Autorité attribuée: {}", authority);

        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }
}