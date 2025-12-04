package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.VendeurApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.LivraisonService;
import sn.votreplateforme.logistique.service.VendeurService;

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
    private final VendeurService vendeurService;

    /**
     * GET /vendeur/dashboard
     * Tableau de bord du vendeur avec statistiques
     */
    @Override
    public ResponseEntity<VendeurDashboard> vendeurDashboardGet() {
        log.info("=== Requête dashboard vendeur ===");

        try {
            VendeurDashboard dashboard = vendeurService.getDashboard();

            log.info("✅ Dashboard généré avec succès");

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du dashboard", e);
            throw new RuntimeException("Erreur lors de la génération du dashboard: " + e.getMessage());
        }
    }

    /**
     * POST /vendeur/demande-paiement
     * Demander un paiement pour récupérer le solde
     */
    @Override
    public ResponseEntity<VendeurDemandePaiementPost200Response> vendeurDemandePaiementPost() {
        log.info("=== Requête demande de paiement ===");

        try {
            VendeurDemandePaiementPost200Response response = vendeurService.demanderPaiement();

            log.info("✅ Demande de paiement créée - Montant: {} FCFA", response.getMontant());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.warn("❌ Demande de paiement refusée: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la demande de paiement", e);
            throw new RuntimeException("Erreur lors de la demande de paiement: " + e.getMessage());
        }
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

        try {
            LivraisonDetailResponse detail = vendeurService.getDetailLivraison(id);

            log.info("✅ Détails récupérés pour livraison {}", id);

            return ResponseEntity.ok(detail);

        } catch (IllegalArgumentException e) {
            log.warn("❌ Livraison {} non trouvée ou accès refusé: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des détails", e);
            throw new RuntimeException("Erreur lors de la récupération des détails: " + e.getMessage());
        }
    }

    /**
     * GET /vendeur/finances
     * État financier du vendeur
     */
    @Override
    public ResponseEntity<VendeurFinances> vendeurFinancesGet() {
        log.info("=== Requête finances vendeur ===");

        try {
            VendeurFinances finances = vendeurService.getFinances();

            log.info("✅ Finances récupérées - Solde: {} FCFA", finances.getSoldeEnAttente());

            return ResponseEntity.ok(finances);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des finances", e);
            throw new RuntimeException("Erreur lors de la récupération des finances: " + e.getMessage());
        }
    }
}