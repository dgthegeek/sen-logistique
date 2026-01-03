package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminZonesApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.ZoneAdminService;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller Admin Zones - Gestion des zones de livraison
 *
 * Endpoints protégés (ROLE_ADMIN requis via @SecurityRequirement dans OpenAPI) :
 * - GET    /admin/zones                - Liste zones avec filtres
 * - POST   /admin/zones                - Créer zone
 * - GET    /admin/zones/{id}           - Détails zone + quartiers
 * - PUT    /admin/zones/{id}           - Modifier zone
 * - DELETE /admin/zones/{id}           - Supprimer zone
 * - PATCH  /admin/zones/{id}/tarifs    - Modifier tarifs
 * - PATCH  /admin/zones/{id}/toggle    - Activer/désactiver
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminZoneController implements AdminZonesApi {

    private final ZoneAdminService zoneAdminService;

    /**
     * GET /admin/zones
     */
    @Override
    public ResponseEntity<PageZone> adminZonesGet(
            Boolean actif,
            String search,
            Integer page,
            Integer size
    ) {
        log.info("📋 Liste zones - Actif: {}, Search: {}, Page: {}", actif, search, page);

        PageZone pageZone = zoneAdminService.getAllZones(actif, search, page, size);

        log.info("✅ {} zones trouvées", pageZone.getContent().size());

        return ResponseEntity.ok(pageZone);
    }

    /**
     * POST /admin/zones
     */
    @Override
    public ResponseEntity<AdminZonesPost201Response> adminZonesPost(
            CreateZoneRequest createZoneRequest
    ) {
        log.info("➕ Création zone : {}", createZoneRequest.getNom());

        AdminZonesPost201Response response = zoneAdminService.createZone(createZoneRequest);

        log.info("✅ Zone créée : {} (ID: {})",
                response.getZone().getNom(),
                response.getZone().getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /admin/zones/{id}
     */
    @Override
    public ResponseEntity<ZoneDetailDTO> adminZonesIdGet(Long id) {
        log.info("📋 Détails zone {}", id);

        ZoneDetailDTO zone = zoneAdminService.getZoneDetail(id);

        log.info("✅ Zone {} - {} quartiers", zone.getNom(), zone.getQuartiers().size());

        return ResponseEntity.ok(zone);
    }

    /**
     * PUT /admin/zones/{id}
     */
    @Override
    public ResponseEntity<AdminZonesIdPut200Response> adminZonesIdPut(
            Long id,
            UpdateZoneRequest updateZoneRequest
    ) {
        log.info("✏️ Modification zone {}", id);

        AdminZonesIdPut200Response response = zoneAdminService.updateZone(id, updateZoneRequest);

        log.info("✅ Zone modifiée : {}", response.getZone().getNom());

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /admin/zones/{id}
     */
    @Override
    public ResponseEntity<AdminZonesIdDelete200Response> adminZonesIdDelete(Long id) {
        log.warn("🗑️ Suppression zone {}", id);

        zoneAdminService.deleteZone(id);

        log.warn("✅ Zone supprimée : {}", id);

        AdminZonesIdDelete200Response response = new AdminZonesIdDelete200Response();
        response.setMessage("Zone supprimée avec succès");

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /admin/zones/{id}/tarifs
     */
    @Override
    public ResponseEntity<AdminZonesIdTarifsPatch200Response> adminZonesIdTarifsPatch(
            Long id,
            UpdateTarifsRequest updateTarifsRequest
    ) {
        log.info("💰 Modification tarifs zone {}", id);

        AdminZonesIdTarifsPatch200Response response =
                zoneAdminService.updateTarifs(id, updateTarifsRequest);

        log.info("✅ Tarifs modifiés - Standard: {}, Express: {}",
                response.getZone().getTarifStandard(),
                response.getZone().getTarifExpress());

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /admin/zones/{id}/toggle
     */
    @Override
    public ResponseEntity<AdminZonesIdTogglePatch200Response> adminZonesIdTogglePatch(Long id) {
        log.info("🔄 Toggle zone {}", id);

        AdminZonesIdTogglePatch200Response response = zoneAdminService.toggleZone(id);

        log.info("✅ {}", response.getMessage());

        return ResponseEntity.ok(response);
    }
}