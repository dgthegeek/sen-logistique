package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminVendeursApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.BilanVendeurPdfService;
import sn.votreplateforme.logistique.service.BilanVendeurService;
import sn.votreplateforme.logistique.service.VendeurAdminService;

import java.time.LocalDate;

/**
 * Controller Admin Vendeurs - Gestion et validation des vendeurs
 *
 * Implémente l'interface AdminVendeursApi générée par OpenAPI
 *
 * Endpoints protégés (ROLE_ADMIN requis via @SecurityRequirement dans OpenAPI) :
 * - GET  /admin/vendeurs/en-attente        - Liste vendeurs à valider
 * - GET  /admin/vendeurs                   - Liste tous vendeurs avec filtres
 * - GET  /admin/vendeurs/{id}              - Détails + stats d'un vendeur
 * - POST /admin/vendeurs/{id}/valider      - Valider un vendeur
 * - POST /admin/vendeurs/{id}/suspendre    - Suspendre un vendeur
 * - POST /admin/vendeurs/{id}/bloquer      - Bloquer définitivement
 * - POST /admin/vendeurs/{id}/reactiver    - Réactiver un suspendu
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminVendeurController implements AdminVendeursApi {

    private final VendeurAdminService vendeurAdminService;
    private final BilanVendeurService bilanVendeurService;
    private final BilanVendeurPdfService bilanVendeurPdfService;
    private final sn.votreplateforme.logistique.service.ClassementService classementService;

    /**
     * GET /admin/vendeurs/en-attente
     */
    @Override
    public ResponseEntity<AdminVendeursEnAttenteGet200Response> adminVendeursEnAttenteGet() {
        log.info("📋 Liste des vendeurs en attente de validation");

        AdminVendeursEnAttenteGet200Response response =
                vendeurAdminService.getVendeursEnAttente();

        log.info("✅ {} vendeurs en attente", response.getTotal());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/vendeurs
     */
    @Override
    public ResponseEntity<PageVendeur> adminVendeursGet(
            StatutVendeur statut,
            String quartier,
            String commune,
            String search,
            String sort,
            String order,
            Integer page,
            Integer size
    ) {
        log.info("📋 Liste vendeurs - Statut: {}, Quartier: {}, Commune: {}, Search: {}",
                statut, quartier, commune, search);

        PageVendeur pageVendeur = vendeurAdminService.getAllVendeurs(
                statut,
                quartier,
                commune,
                search,
                sort,
                order,
                page,
                size
        );

        log.info("✅ {} vendeurs trouvés (page {}/{})",
                pageVendeur.getContent().size(),
                pageVendeur.getPage() + 1,
                pageVendeur.getTotalPages()
        );

        return ResponseEntity.ok(pageVendeur);
    }

    /**
     * GET /admin/vendeurs/{id}
     */
    @Override
    public ResponseEntity<VendeurDetailDTO> adminVendeursIdGet(Long id) {
        log.info("📋 Détails vendeur {}", id);

        VendeurDetailDTO vendeur = vendeurAdminService.getVendeurDetail(id);

        log.info("✅ Vendeur {} - Statut: {}, Livraisons: {}",
                id,
                vendeur.getStatut(),
                vendeur.getStatistiques() != null ? vendeur.getStatistiques().getNombreLivraisons() : 0
        );

        return ResponseEntity.ok(vendeur);
    }

    /**
     * POST /admin/vendeurs/{id}/valider
     */
    @Override
    public ResponseEntity<AdminVendeursIdValiderPost200Response> adminVendeursIdValiderPost(
            Long id,
            AdminVendeursIdValiderPostRequest adminVendeursIdValiderPostRequest) {
        java.math.BigDecimal commission = adminVendeursIdValiderPostRequest != null
                ? adminVendeursIdValiderPostRequest.getCommission()
                : null;
        log.info("✅ Validation vendeur {} - Commission: {}", id, commission);

        AdminVendeursIdValiderPost200Response response =
                vendeurAdminService.validerVendeur(id, commission);

        log.info("✅ Vendeur {} validé avec succès", id);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /admin/vendeurs/{id}/commission - Régler/modifier la commission fixe (prix de livraison) du vendeur
     */
    @Override
    public ResponseEntity<VendeurDTO> adminSetCommission(
            Long id,
            AdminSetCommissionRequest adminSetCommissionRequest) {
        log.info("💰 Commission vendeur {} = {}", id, adminSetCommissionRequest.getCommission());

        VendeurDTO vendeur = vendeurAdminService.setCommission(id, adminSetCommissionRequest.getCommission());

        return ResponseEntity.ok(vendeur);
    }

    /**
     * POST /admin/vendeurs/{id}/suspendre
     */
    @Override
    public ResponseEntity<AdminVendeursIdSuspendrePost200Response> adminVendeursIdSuspendrePost(
            Long id,
            AdminVendeursIdSuspendrePostRequest request
    ) {
        log.info("⏸️ Suspension vendeur {} - Raison: {}", id, request.getRaison());

        AdminVendeursIdSuspendrePost200Response response =
                vendeurAdminService.suspendreVendeur(id, request.getRaison());

        log.info("✅ Vendeur {} suspendu", id);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /admin/vendeurs/{id}/bloquer
     */
    @Override
    public ResponseEntity<AdminVendeursIdBloquerPost200Response> adminVendeursIdBloquerPost(
            Long id,
            AdminVendeursIdBloquerPostRequest request
    ) {
        log.warn("🚫 Blocage vendeur {} - Raison: {}", id, request.getRaison());

        AdminVendeursIdBloquerPost200Response response =
                vendeurAdminService.bloquerVendeur(id, request.getRaison());

        log.warn("✅ Vendeur {} bloqué définitivement", id);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /admin/vendeurs/{id}/reactiver
     */
    @Override
    public ResponseEntity<AdminVendeursIdReactiverPost200Response> adminVendeursIdReactiverPost(Long id) {
        log.info("♻️ Réactivation vendeur {}", id);

        AdminVendeursIdReactiverPost200Response response =
                vendeurAdminService.reactiverVendeur(id);

        log.info("✅ Vendeur {} réactivé", id);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/vendeurs/{id}/bilan
     */
    @Override
    public ResponseEntity<BilanVendeur> adminVendeurBilan(Long id, LocalDate debut, LocalDate fin) {
        log.info("📊 Bilan vendeur {} ({} → {})", id, debut, fin);
        return ResponseEntity.ok(bilanVendeurService.getBilanVendeur(id, debut, fin));
    }

    /**
     * GET /admin/vendeurs/{id}/bilan/pdf
     */
    @Override
    public ResponseEntity<org.springframework.core.io.Resource> adminVendeurBilanPdf(
            Long id, LocalDate debut, LocalDate fin) {
        BilanVendeur bilan = bilanVendeurService.getBilanVendeur(id, debut, fin);
        byte[] pdf = bilanVendeurPdfService.generer(bilan);
        String boutique = bilan.getVendeur() != null && bilan.getVendeur().getNomBoutique() != null
                ? bilan.getVendeur().getNomBoutique().replaceAll("[^a-zA-Z0-9-_]", "_")
                : "vendeur";
        String filename = String.format("bilan-%s-%s.pdf", boutique, bilan.getPeriodeDebut());

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers)
                .body(new org.springframework.core.io.ByteArrayResource(pdf));
    }

    /**
     * GET /admin/classement - Dioks League complète (tous les vendeurs)
     */
    @Override
    public ResponseEntity<ClassementResponse> adminClassement() {
        return ResponseEntity.ok(classementService.getClassementAdmin());
    }
}