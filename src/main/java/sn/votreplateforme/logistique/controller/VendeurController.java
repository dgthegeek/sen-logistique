package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.VendeurApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.exception.ForbiddenException;
import sn.votreplateforme.logistique.repository.VendeurRepository;
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
    private final VendeurRepository vendeurRepository;

    /**
     * GET /vendeur/dashboard
     * Tableau de bord du vendeur avec statistiques
     */
    @Override
    public ResponseEntity<VendeurDashboard> vendeurDashboardGet() {
        log.info("=== Requête dashboard vendeur ===");

        try {
            // Vérifier le statut du vendeur
            verifierVendeurActif();

            VendeurDashboard dashboard = vendeurService.getDashboard();

            log.info("✅ Dashboard généré avec succès");

            return ResponseEntity.ok(dashboard);

        } catch (ForbiddenException e) {
            log.warn("❌ Accès refusé: {}", e.getMessage());
            throw e;
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
            // Vérifier le statut du vendeur
            verifierVendeurActif();

            VendeurDemandePaiementPost200Response response = vendeurService.demanderPaiement();

            log.info("✅ Demande de paiement créée - Montant: {} FCFA", response.getMontant());

            return ResponseEntity.ok(response);

        } catch (ForbiddenException e) {
            log.warn("❌ Accès refusé: {}", e.getMessage());
            throw e;
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
            // Vérifier le statut du vendeur
//            verifierVendeurActif();

            LivraisonResponse response = livraisonService.creerLivraison(createLivraisonRequest);

            log.info("✅ Livraison créée: {}", response.getNumeroTracking());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (ForbiddenException e) {
            log.warn("❌ Accès refusé: {}", e.getMessage());
            throw e;
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
            // Vérifier le statut du vendeur
            verifierVendeurActif();

            // Valeurs par défaut
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

            PageLivraison pageLivraison = livraisonService.getMesLivraisons(
                    statut, pageNumber, pageSize);

            log.info("✅ Livraisons récupérées: {} éléments", pageLivraison.getContent().size());

            return ResponseEntity.ok(pageLivraison);

        } catch (ForbiddenException e) {
            log.warn("❌ Accès refusé: {}", e.getMessage());
            throw e;
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
            // Vérifier le statut du vendeur
            verifierVendeurActif();

            LivraisonDetailResponse detail = vendeurService.getDetailLivraison(id);

            log.info("✅ Détails récupérés pour livraison {}", id);

            return ResponseEntity.ok(detail);

        } catch (ForbiddenException e) {
            log.warn("❌ Accès refusé: {}", e.getMessage());
            throw e;
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
            // Vérifier le statut du vendeur
            verifierVendeurActif();

            VendeurFinances finances = vendeurService.getFinances();

            log.info("✅ Finances récupérées - Solde: {} FCFA", finances.getSoldeEnAttente());

            return ResponseEntity.ok(finances);

        } catch (ForbiddenException e) {
            log.warn("❌ Accès refusé: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des finances", e);
            throw new RuntimeException("Erreur lors de la récupération des finances: " + e.getMessage());
        }
    }

    // ===== MÉTHODE PRIVÉE DE VÉRIFICATION =====

    /**
     * Vérifie que le vendeur connecté a le statut ACTIF
     * Lève une ForbiddenException sinon
     */
    private void verifierVendeurActif() {
        // Récupérer l'utilisateur connecté depuis le SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String telephone = authentication.getName(); // Le téléphone est le username

        // Charger le vendeur depuis la DB
        Vendeur vendeur = vendeurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));

        // Vérifier le statut
        if (vendeur.getStatut() != StatutVendeur.ACTIF) {
            String message = switch (vendeur.getStatut()) {
                case EN_ATTENTE_VALIDATION ->
                        "Votre compte est en cours de validation. Vous pourrez utiliser la plateforme une fois votre compte validé par notre équipe.";
                case SUSPENDU ->
                        "Votre compte est temporairement suspendu. " +
                                (vendeur.getRaisonSuspension() != null ? "Raison: " + vendeur.getRaisonSuspension() : "");
                case BLOQUE ->
                        "Votre compte a été bloqué. " +
                                (vendeur.getRaisonSuspension() != null ? "Raison: " + vendeur.getRaisonSuspension() : "");
                default -> "Votre compte n'est pas actif.";
            };
            throw new ForbiddenException(message);
        }
    }
}