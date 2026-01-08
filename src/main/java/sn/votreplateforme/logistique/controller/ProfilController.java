package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.ProfilApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.ProfilService;

import java.util.HashMap;
import java.util.Map;

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
}