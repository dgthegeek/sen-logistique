package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminQuartiersApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.QuartierAdminService;

/**
 * Controller Admin Quartiers - Gestion des quartiers
 *
 * Endpoints protégés (ROLE_ADMIN requis) :
 * - POST   /admin/quartiers           - Créer quartier
 * - PUT    /admin/quartiers/{id}      - Modifier quartier
 * - DELETE /admin/quartiers/{id}      - Supprimer quartier
 * - PATCH  /admin/quartiers/{id}/toggle - Activer/désactiver
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminQuartierController implements AdminQuartiersApi {

    private final QuartierAdminService quartierAdminService;

    /**
     * POST /admin/quartiers
     */
    @Override
    public ResponseEntity<AdminQuartiersPost201Response> adminQuartiersPost(
            CreateQuartierRequest createQuartierRequest
    ) {
        log.info("➕ Création quartier : {} ({})",
                createQuartierRequest.getNom(),
                createQuartierRequest.getCommune());

        AdminQuartiersPost201Response response =
                quartierAdminService.createQuartier(createQuartierRequest);

        log.info("✅ Quartier créé : {}", response.getQuartier().getNom());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /admin/quartiers/{id}
     */
    @Override
    public ResponseEntity<AdminQuartiersIdPut200Response> adminQuartiersIdPut(
            Long id,
            UpdateQuartierRequest updateQuartierRequest
    ) {
        log.info("✏️ Modification quartier {}", id);

        AdminQuartiersIdPut200Response response =
                quartierAdminService.updateQuartier(id, updateQuartierRequest);

        log.info("✅ Quartier modifié : {}", response.getQuartier().getNom());

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /admin/quartiers/{id}
     */
    @Override
    public ResponseEntity<AdminQuartiersIdDelete200Response> adminQuartiersIdDelete(Long id) {
        log.warn("🗑️ Suppression quartier {}", id);

        AdminQuartiersIdDelete200Response response = quartierAdminService.deleteQuartier(id);

        log.warn("✅ {}", response.getMessage());

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /admin/quartiers/{id}/toggle
     */
    @Override
    public ResponseEntity<AdminQuartiersIdTogglePatch200Response> adminQuartiersIdTogglePatch(Long id) {
        log.info("🔄 Toggle quartier {}", id);

        AdminQuartiersIdTogglePatch200Response response = quartierAdminService.toggleQuartier(id);

        log.info("✅ {}", response.getMessage());

        return ResponseEntity.ok(response);
    }
}