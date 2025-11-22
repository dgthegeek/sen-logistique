package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.DeliveryApi;
import sn.votreplateforme.logistique.dto.ConfirmLivraisonRequest;
import sn.votreplateforme.logistique.dto.DeliveryInfoResponse;
import sn.votreplateforme.logistique.dto.DeliveryNumeroTrackingLivrerPost200Response;
import sn.votreplateforme.logistique.service.DeliveryService;

import java.util.Map;

/**
 * Controller Delivery - Gestion de la confirmation de livraison via scan QR
 *
 * ImplÃ©mente l'interface DeliveryApi gÃ©nÃ©rÃ©e par OpenAPI
 *
 * Endpoints PUBLICS (pas d'authentification requise) :
 * - GET  /delivery/{numeroTracking}         - RÃ©cupÃ¨re les infos pour le formulaire
 * - POST /delivery/{numeroTracking}/livrer  - Confirme la livraison
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class DeliveryController implements DeliveryApi {

    private final DeliveryService deliveryService;

    /**
     * GET /delivery/{numeroTracking}
     *
     * RÃ©cupÃ¨re les informations de livraison aprÃ¨s scan du QR code.
     * Affiche un formulaire prÃ©-rempli avec les dÃ©tails du colis.
     *
     * @param numeroTracking NumÃ©ro de tracking (ex: DKR-00567)
     * @return DeliveryInfoResponse avec toutes les infos
     */
    @Override
    public ResponseEntity<DeliveryInfoResponse> deliveryNumeroTrackingGet(String numeroTracking) {
        log.info("ðŸ“± Scan QR code - RÃ©cupÃ©ration infos livraison: {}", numeroTracking);

        DeliveryInfoResponse response = deliveryService.getDeliveryInfo(numeroTracking);

        log.info("âœ… Infos livraison {} rÃ©cupÃ©rÃ©es avec succÃ¨s", numeroTracking);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /delivery/{numeroTracking}/livrer
     *
     * Confirme la livraison du colis.
     * Change le statut RAMASSE â†’ LIVREE
     * Enregistre le cash collectÃ©
     * Envoie les notifications automatiques
     *
     * @param numeroTracking NumÃ©ro de tracking
     * @param confirmLivraisonRequest DonnÃ©es de confirmation (cash, commentaire)
     * @return Message de confirmation
     */
    @Override
    public ResponseEntity<DeliveryNumeroTrackingLivrerPost200Response> deliveryNumeroTrackingLivrerPost(
            String numeroTracking,
            ConfirmLivraisonRequest confirmLivraisonRequest
    ) {
        log.info("ðŸ“¦ Confirmation livraison: {} - Cash: {} FCFA",
                numeroTracking,
                confirmLivraisonRequest.getCashCollecte()
        );

        DeliveryNumeroTrackingLivrerPost200Response response = new DeliveryNumeroTrackingLivrerPost200Response();


        deliveryService.confirmerLivraison(
                numeroTracking,
                confirmLivraisonRequest
        );

        log.info("âœ… Livraison {} confirmÃ©e avec succÃ¨s", numeroTracking);
        return ResponseEntity.ok(response);
    }
}