package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.PartnerApi;
import sn.votreplateforme.logistique.dto.CreateLivraisonRequest;
import sn.votreplateforme.logistique.dto.LivraisonResponse;
import sn.votreplateforme.logistique.dto.ShopifyOrderWebhook;
import sn.votreplateforme.logistique.service.PartnerApiService;

/**
 * Création de commande pour un partenaire externe (Shopify, script, Zapier...).
 *
 * <p>Ces endpoints sont publics au sens Spring Security (pas de session JWT
 * requise, voir {@code SecurityConfig} : {@code /partner/**} en
 * {@code permitAll}) — l'authentification se fait "à la main" via une clé API
 * propre à chaque vendeur, vérifiée dans {@link PartnerApiService}. C'est le
 * même principe qu'un webhook signé : une URL techniquement accessible, mais
 * dont chaque appel doit prouver qu'il connaît un secret.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class PartnerOrderController implements PartnerApi {

    private final PartnerApiService partnerApiService;

    @Override
    public ResponseEntity<LivraisonResponse> partnerCommandesPost(
            String xApiKey, CreateLivraisonRequest createLivraisonRequest, String xIdempotencyKey) {
        LivraisonResponse response = partnerApiService.creerCommande(xApiKey, xIdempotencyKey, createLivraisonRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<LivraisonResponse> partnerShopifyCommandesPost(
            String key, ShopifyOrderWebhook shopifyOrderWebhook) {
        LivraisonResponse response = partnerApiService.creerCommandeDepuisShopify(key, shopifyOrderWebhook);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
