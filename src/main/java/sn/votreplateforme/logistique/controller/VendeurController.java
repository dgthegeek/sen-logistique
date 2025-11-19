package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.VendeurApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.service.LivraisonService;

/**
 * Controller Vendeur
 * Implémente l'interface VendeurApi générée par OpenAPI
 *
 * Endpoints:
 * - GET /vendeur/dashboard - Tableau de bord
 * - POST /vendeur/livraisons - Créer une livraison
 * - GET /vendeur/livraisons - Liste des livraisons
 * - GET /vendeur/livraisons/{id} - Détails d'une livraison
 * - GET /vendeur/finances - État financier
 * - POST /vendeur/demande-paiement - Demander un paiement
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class VendeurController implements VendeurApi {

    private final LivraisonService livraisonService;

    /**
     * GET /vendeur/dashboard
     * Tableau de bord du vendeur avec statistiques
     */
    @Override
    public ResponseEntity<VendeurDashboard> vendeurDashboardGet() {
        log.info("=== Requête dashboard vendeur ===");

        // TODO: Implémenter le service de dashboard
        throw new UnsupportedOperationException("Dashboard vendeur pas encore implémenté");
    }

    @Override
    public ResponseEntity<VendeurDemandePaiementPost200Response> vendeurDemandePaiementPost() {
        return null;
    }

    /**
     * POST /vendeur/livraisons
     * Créer une nouvelle livraison
     */
    @Override
    public ResponseEntity<LivraisonResponse> vendeurLivraisonsPost(
            CreateLivraisonRequest createLivraisonRequest) {

        log.info("=== Requête création de livraison ===");
        log.debug("Client: {} - Destination: {}",
                createLivraisonRequest.getNomClient(),
                createLivraisonRequest.getQuartier());

        try {
            LivraisonResponse response = livraisonService.creerLivraison(createLivraisonRequest);

            log.info("✅ Livraison créée: {}", response.getNumeroTracking());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {
            log.warn("❌ Erreur création livraison: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la création", e);
            throw new RuntimeException("Erreur lors de la création de la livraison: " + e.getMessage());
        }
    }

    /**
     * GET /vendeur/livraisons
     * Liste des livraisons du vendeur avec pagination
     */
    @Override
    public ResponseEntity<PageLivraison> vendeurLivraisonsGet(
            sn.votreplateforme.logistique.dto.StatutLivraison statut,
            Integer page,
            Integer size) {

        log.info("=== Requête liste livraisons vendeur ===");
        log.debug("Statut: {}, Page: {}, Size: {}", statut, page, size);

        try {
            // Valeurs par défaut
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

            PageLivraison pageLivraison = livraisonService.getMesLivraisons(
                    statut, pageNumber, pageSize);

            log.info("✅ Livraisons récupérées: {} éléments", pageLivraison.getContent().size());

            return ResponseEntity.ok(pageLivraison);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des livraisons", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * GET /vendeur/livraisons/{id}
     * Détails d'une livraison
     */
    @Override
    public ResponseEntity<LivraisonDetailResponse> vendeurLivraisonsIdGet(Long id) {
        log.info("=== Requête détails livraison {} ===", id);

        // TODO: Implémenter la récupération des détails
        throw new UnsupportedOperationException("Détails livraison pas encore implémenté");
    }

    /**
     * GET /vendeur/finances
     * État financier du vendeur
     */
    @Override
    public ResponseEntity<VendeurFinances> vendeurFinancesGet() {
        log.info("=== Requête finances vendeur ===");

        // TODO: Implémenter le service de finances
        throw new UnsupportedOperationException("Finances vendeur pas encore implémenté");
    }


}