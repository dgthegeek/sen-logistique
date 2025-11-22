package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.TrackingApi;
import sn.votreplateforme.logistique.dto.TrackingResponse;
import sn.votreplateforme.logistique.service.TrackingService;

/**
 * Controller Tracking - Suivi public des colis
 *
 * ImplÃ©mente l'interface TrackingApi gÃ©nÃ©rÃ©e par OpenAPI
 *
 * Endpoint PUBLIC (pas d'authentification requise) :
 * - GET /tracking/{numeroTracking} - Page de suivi public pour les clients
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class TrackingController implements TrackingApi {

    private final TrackingService trackingService;

    /**
     * GET /tracking/{numeroTracking}
     *
     * Page de tracking publique accessible par le client final.
     * Affiche l'historique complet du colis avec timeline.
     *
     * Pas d'authentification requise - endpoint public.
     *
     * @param numeroTracking NumÃ©ro de tracking (ex: DKR-00567)
     * @return TrackingResponse avec timeline et statut actuel
     */
    @Override
    public ResponseEntity<TrackingResponse> trackingNumeroTrackingGet(String numeroTracking) {
        log.info("ðŸ” Consultation tracking public: {}", numeroTracking);

        TrackingResponse response = trackingService.getTrackingInfo(numeroTracking);

        log.info("âœ… Tracking {} rÃ©cupÃ©rÃ© - Statut: {}",
                numeroTracking,
                response.getStatut()
        );

        return ResponseEntity.ok(response);
    }
}