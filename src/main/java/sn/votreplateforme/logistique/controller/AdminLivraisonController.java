package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminLivraisonsApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.AdminLivraisonService;

import java.time.LocalDate;

/**
 * Controller Admin Livraisons
 *
 * Endpoints:
 * - GET /admin/livraisons - Liste toutes les livraisons
 * - GET /admin/livraisons/a-livrer - Colis à livrer (RAMASSE)
 * - GET /admin/livraisons/{id} - Détails d'une livraison
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminLivraisonController implements AdminLivraisonsApi {

    private final AdminLivraisonService adminLivraisonService;

    /**
     * GET /admin/livraisons
     * Liste de toutes les livraisons avec pagination et filtres
     */
    @Override
    public ResponseEntity<PageLivraison> adminLivraisonsGet(
            sn.votreplateforme.logistique.dto.StatutLivraison statut,
            LocalDate date,
            Integer page,
            Integer size
    ) {
        log.info("📦 Liste livraisons admin - Statut: {}, Date: {}, Page: {}",
                statut, date, page);

        PageLivraison response = adminLivraisonService.getAllLivraisons(
                statut, date, page, size);

        log.info("✅ {} livraisons récupérées", response.getContent().size());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/livraisons/a-livrer
     * Colis à livrer (statut RAMASSE), groupés par zone
     */
    @Override
    public ResponseEntity<AdminLivraisonsALivrerGet200Response> adminLivraisonsALivrerGet(
            String zone
    ) {
        log.info("📦 Colis à livrer - Zone: {}", zone);

        AdminLivraisonsALivrerGet200Response response =
                adminLivraisonService.getColisALivrer(zone);

        log.info("✅ {} colis à livrer dans {} zones",
                response.getTotalColis(),
                response.getZones().size());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/livraisons/{id}
     * Détails complets d'une livraison (admin)
     */
    @Override
    public ResponseEntity<LivraisonDetailResponse> adminLivraisonsIdGet(Long id) {
        log.info("📦 Détails livraison {} (admin)", id);

        LivraisonDetailResponse response = adminLivraisonService.getDetailLivraison(id);

        log.info("✅ Détails livraison {} récupérés", response.getNumeroTracking());

        return ResponseEntity.ok(response);
    }
}