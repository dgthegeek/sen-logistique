package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Admin;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Récupère l'utilisateur connecté
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String telephone = auth.getName();

        return userRepository.findByTelephone(telephone)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));
    }

    /**
     * GET /profil
     */
    @Transactional(readOnly = true)
    public ProfilResponse getProfil() {
        User user = getCurrentUser();

        log.info("📋 Récupération profil : {} ({})", user.getTelephone(), user.getRole());

        return toProfilResponse(user);
    }

    /**
     * PUT /profil
     */
    @Transactional
    public ProfilPut200Response updateProfil(UpdateProfilRequest request) {
        User user = getCurrentUser();

        log.info("✏️ Modification profil : {}", user.getTelephone());

        // Mise à jour champs communs
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());

        // Si vendeur, mettre à jour champs spécifiques
        if (user instanceof Vendeur vendeur) {
            if (request.getNomBoutique() != null) {
                vendeur.setNomBoutique(request.getNomBoutique());
            }
            if (request.getCategorieActivite() != null) {
                vendeur.setCategorieActivite(request.getCategorieActivite());
            }
            if (request.getInstagram() != null) {
                vendeur.setInstagram(request.getInstagram());
            }
            if (request.getFacebook() != null) {
                vendeur.setFacebook(request.getFacebook());
            }
            if (request.getCommune() != null) {
                vendeur.setCommune(request.getCommune());
            }
            if (request.getQuartier() != null) {
                vendeur.setQuartier(request.getQuartier());
            }
            if (request.getAdresseComplete() != null) {
                vendeur.setAdresseComplete(request.getAdresseComplete());
            }
        }

        user = userRepository.save(user);

        log.info("✅ Profil mis à jour : {}", user.getTelephone());

        ProfilPut200Response response = new ProfilPut200Response();
        response.setMessage("Profil mis à jour avec succès");
        response.setProfil(toProfilResponse(user));

        return response;
    }

    /**
     * PATCH /profil/password
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        log.info("🔐 Changement mot de passe : {}", user.getTelephone());

        // Vérifier ancien mot de passe
        if (!passwordEncoder.matches(request.getAncienPassword(), user.getPassword())) {
            throw new BadRequestException("Ancien mot de passe incorrect");
        }

        // Vérifier que nouveau != ancien
        if (request.getAncienPassword().equals(request.getNouveauPassword())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Mettre à jour
        user.setPassword(passwordEncoder.encode(request.getNouveauPassword()));
        userRepository.save(user);

        log.info("✅ Mot de passe modifié : {}", user.getTelephone());
    }

    // ===== MAPPER =====

    private ProfilResponse toProfilResponse(User user) {
        ProfilResponse dto = new ProfilResponse();

        // Champs communs
        dto.setId(user.getId());
        dto.setNom(user.getNom());
        dto.setPrenom(user.getPrenom());
        dto.setTelephone(user.getTelephone());
        dto.setEmail(user.getEmail());
        dto.setRole(ProfilResponse.RoleEnum.valueOf(user.getRole().name()));

        if (user.getDateInscription() != null) {
            dto.setDateInscription(user.getDateInscription().atOffset(ZoneOffset.UTC));
        }

        // Champs spécifiques vendeur
        if (user instanceof Vendeur vendeur) {
            dto.setNomBoutique(vendeur.getNomBoutique());
            dto.setCategorieActivite(vendeur.getCategorieActivite());
            dto.setInstagram(vendeur.getInstagram());
            dto.setFacebook(vendeur.getFacebook());
            dto.setCommune(vendeur.getCommune());
            dto.setQuartier(vendeur.getQuartier());
            dto.setAdresseComplete(vendeur.getAdresseComplete());
            dto.setStatut(StatutVendeur.fromValue(vendeur.getStatut().name()));
            dto.setSoldeEnAttente(vendeur.getSoldeEnAttente());
        }

        return dto;
    }
}