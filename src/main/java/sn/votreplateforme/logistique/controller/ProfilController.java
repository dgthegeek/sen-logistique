package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.ProfilApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.UserRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;
import sn.votreplateforme.logistique.service.ProfilService;
import sn.votreplateforme.logistique.service.TelegramService;

/**
 * Controller Profil - Gestion du profil utilisateur
 *
 * Endpoints protégés (authentification requise) :
 * - GET   /profil          - Mon profil
 * - PUT   /profil          - Modifier mon profil
 * - PATCH /profil/password - Changer mot de passe
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ProfilController implements ProfilApi {

    private final ProfilService profilService;
    private final TelegramService telegramService;
    private final UserRepository userRepository;

    /** Utilisateur connecté (tous rôles). */
    private User getCurrentUser() {
        return userRepository.findByTelephone(SecurityUtils.getCurrentUserTelephone())
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));
    }

    /**
     * GET /profil
     */
    @Override
    public ResponseEntity<ProfilResponse> profilGet() {
        log.info("📋 GET /profil");

        ProfilResponse profil = profilService.getProfil();

        log.info("✅ Profil récupéré : {} {}", profil.getPrenom(), profil.getNom());

        return ResponseEntity.ok(profil);
    }

    /**
     * PUT /profil
     */
    @Override
    public ResponseEntity<ProfilPut200Response> profilPut(UpdateProfilRequest updateProfilRequest) {
        log.info("✏️ PUT /profil");

        ProfilPut200Response response = profilService.updateProfil(updateProfilRequest);

        log.info("✅ {}", response.getMessage());

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /profil/password
     */
    @Override
    public ResponseEntity<ProfilPasswordPatch200Response> profilPasswordPatch(
            ChangePasswordRequest changePasswordRequest
    ) {
        log.info("🔐 PATCH /profil/password");

        profilService.changePassword(changePasswordRequest);

        log.info("✅ Mot de passe modifié");

        ProfilPasswordPatch200Response response = new ProfilPasswordPatch200Response();
        response.setMessage("Mot de passe modifié avec succès");

        return ResponseEntity.ok(response);
    }

    /**
     * GET /profil/telegram - Statut de liaison Telegram (tous rôles)
     */
    @Override
    public ResponseEntity<TelegramStatut> profilTelegram() {
        return ResponseEntity.ok(telegramService.getStatut(getCurrentUser()));
    }

    /**
     * POST /profil/telegram/delier - Délier le compte Telegram (tous rôles)
     */
    @Override
    public ResponseEntity<TelegramStatut> profilTelegramDelier() {
        return ResponseEntity.ok(telegramService.delier(getCurrentUser()));
    }
}