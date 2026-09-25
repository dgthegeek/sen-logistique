package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.ApiKeyResponse;
import sn.votreplateforme.logistique.dto.CreateLivraisonRequest;
import sn.votreplateforme.logistique.dto.LivraisonResponse;
import sn.votreplateforme.logistique.dto.ShopifyAddress;
import sn.votreplateforme.logistique.dto.ShopifyCustomer;
import sn.votreplateforme.logistique.dto.ShopifyLineItem;
import sn.votreplateforme.logistique.dto.ShopifyOrderWebhook;
import sn.votreplateforme.logistique.dto.StatutVendeur;
import sn.votreplateforme.logistique.entity.Produit;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.ForbiddenException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.ProduitRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.util.ApiKeyGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Intégrations partenaires : gestion de la clé API des vendeurs et création
 * de commande pour leur compte sans session (script externe, Shopify...).
 *
 * <p>Une clé API remplace la connexion par mot de passe pour un système tiers :
 * elle identifie le vendeur au même titre qu'une session JWT, mais reste
 * statique tant qu'elle n'est pas régénérée. Toute la logique métier de
 * création de commande (calcul de la commission, décrément de stock, entrée
 * en file de closing, notifications...) est réutilisée telle quelle via
 * {@link LivraisonService#creerLivraisonPourVendeur}, garantissant qu'une
 * commande créée par une intégration se comporte exactement comme si le
 * vendeur l'avait créée lui-même depuis son espace.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerApiService {

    private final VendeurRepository vendeurRepository;
    private final ProduitRepository produitRepository;
    private final LivraisonService livraisonService;

    // ==================== GESTION DE LA CLÉ ====================

    /** Clé API actuelle du vendeur (null si aucune intégration configurée). */
    @Transactional(readOnly = true)
    public ApiKeyResponse getStatutCle(Vendeur vendeur) {
        ApiKeyResponse dto = new ApiKeyResponse();
        dto.setApiKey(vendeur.getApiKey());
        return dto;
    }

    /** Génère une nouvelle clé pour le vendeur, invalidant l'ancienne si elle existait. */
    @Transactional
    public ApiKeyResponse regenererCle(Vendeur vendeur) {
        String cle = ApiKeyGenerator.genererCle();
        vendeur.setApiKey(cle);
        vendeurRepository.save(vendeur);
        log.info("🔑 Nouvelle clé API générée pour le vendeur {} (id={})",
                vendeur.getNomComplet(), vendeur.getId());
        ApiKeyResponse dto = new ApiKeyResponse();
        dto.setApiKey(cle);
        return dto;
    }

    /** Même opération que {@link #getStatutCle}, en résolvant le vendeur par son ID (support admin). */
    @Transactional(readOnly = true)
    public ApiKeyResponse getStatutCle(Long vendeurId) {
        return getStatutCle(getVendeurOuLever(vendeurId));
    }

    /** Même opération que {@link #regenererCle}, en résolvant le vendeur par son ID (support admin). */
    @Transactional
    public ApiKeyResponse regenererCle(Long vendeurId) {
        return regenererCle(getVendeurOuLever(vendeurId));
    }

    private Vendeur getVendeurOuLever(Long vendeurId) {
        return vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé : " + vendeurId));
    }

    // ==================== CRÉATION DE COMMANDE ====================

    /** Intégration générique (script maison, Zapier, Make...). */
    @Transactional
    public LivraisonResponse creerCommande(String apiKey, String idempotencyKey, CreateLivraisonRequest request) {
        Vendeur vendeur = resoudreVendeurParCle(apiKey);
        return livraisonService.creerLivraisonPourVendeur(vendeur, request, "API", idempotencyKey);
    }

    /** Adaptateur webhook Shopify "orders/create". */
    @Transactional
    public LivraisonResponse creerCommandeDepuisShopify(String apiKey, ShopifyOrderWebhook payload) {
        Vendeur vendeur = resoudreVendeurParCle(apiKey);
        CreateLivraisonRequest request = mapperShopify(vendeur, payload);
        String origineRef = payload.getId() != null ? String.valueOf(payload.getId()) : null;
        return livraisonService.creerLivraisonPourVendeur(vendeur, request, "SHOPIFY", origineRef);
    }

    /**
     * Résout le vendeur propriétaire d'une clé API et vérifie que son compte
     * est actif — une intégration externe ne doit pas pouvoir créer des
     * commandes pour un vendeur suspendu ou bloqué.
     */
    private Vendeur resoudreVendeurParCle(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ForbiddenException("Clé API manquante");
        }
        Vendeur vendeur = vendeurRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ForbiddenException("Clé API invalide"));

        if (vendeur.getStatut() != StatutVendeur.ACTIF) {
            throw new ForbiddenException("Ce compte partenaire n'est pas actif");
        }
        return vendeur;
    }

    /**
     * Convertit le JSON natif d'une commande Shopify en requête de création
     * de livraison Dioks. Les adresses Dakar/quartier/zone sont volontairement
     * laissées vides : Shopify ne connaît pas notre découpage par quartier, la
     * commande passe donc par le tarif "adresse libre" (commission fixe du
     * vendeur), déjà géré nativement par {@link LivraisonService}.
     */
    private CreateLivraisonRequest mapperShopify(Vendeur vendeur, ShopifyOrderWebhook payload) {
        ShopifyAddress adresse = payload.getShippingAddress();
        ShopifyCustomer client = payload.getCustomer();

        CreateLivraisonRequest request = new CreateLivraisonRequest();

        String prenom = premierNonVide(
                adresse != null ? adresse.getFirstName() : null,
                client != null ? client.getFirstName() : null);
        String nom = premierNonVide(
                adresse != null ? adresse.getLastName() : null,
                client != null ? client.getLastName() : null);
        String nomComplet = ((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim();
        request.setNomClient(!nomComplet.isBlank() ? nomComplet : "Client Shopify");

        String telephone = premierNonVide(
                adresse != null ? adresse.getPhone() : null,
                client != null ? client.getPhone() : null,
                payload.getPhone());
        if (telephone == null || telephone.isBlank()) {
            throw new BadRequestException(
                    "Commande Shopify " + libelleCommande(payload)
                            + " sans numéro de téléphone client : impossible de créer la livraison.");
        }
        request.setTelephoneClient(nettoyerTelephone(telephone));

        if (adresse != null) {
            String adresseComplete = adresse.getAddress1() != null ? adresse.getAddress1() : "";
            if (adresse.getAddress2() != null && !adresse.getAddress2().isBlank()) {
                adresseComplete = adresseComplete.isBlank()
                        ? adresse.getAddress2()
                        : adresseComplete + ", " + adresse.getAddress2();
            }
            request.setAdresseComplete(!adresseComplete.isBlank()
                    ? adresseComplete : "Adresse non renseignée (commande Shopify)");
            request.setCommune(adresse.getCity());
        } else {
            request.setAdresseComplete("Adresse non renseignée (commande Shopify)");
        }

        request.setDescriptionProduit(descriptionProduits(payload));
        request.setItems(resoudreLignesCatalogue(vendeur, payload));
        // montantCOD volontairement non renseigné : le calcul (prix catalogue
        // Dioks × quantité + frais de livraison) est fait par la même logique
        // que pour une commande créée depuis la plateforme — voir plus bas.

        request.setNotesPourLivreur(payload.getNote());
        request.setFragile(false);

        return request;
    }

    /**
     * Résout chaque ligne Shopify vers un produit du catalogue Dioks du
     * vendeur, via son SKU (= code produit Dioks, ex. "DKS-00042").
     *
     * <p>Tout ou rien : si une seule ligne n'a pas de SKU, ou un SKU qui ne
     * correspond à aucun produit de <b>ce</b> vendeur, toute la commande est
     * refusée (aucune livraison créée, aucun impact stock/prix). Une commande
     * acceptée est ensuite traitée exactement comme une commande multi-produits
     * créée depuis la plateforme : prix = celui enregistré dans Dioks, stock
     * vérifié et décrémenté pour chaque ligne.
     */
    private List<sn.votreplateforme.logistique.dto.LigneCommandeRequest> resoudreLignesCatalogue(
            Vendeur vendeur, ShopifyOrderWebhook payload) {
        List<ShopifyLineItem> lignesShopify = payload.getLineItems();
        if (lignesShopify == null || lignesShopify.isEmpty()) {
            throw new BadRequestException(
                    "Commande Shopify " + libelleCommande(payload) + " sans article.");
        }

        List<sn.votreplateforme.logistique.dto.LigneCommandeRequest> lignes = new java.util.ArrayList<>();
        for (ShopifyLineItem ligneShopify : lignesShopify) {
            String sku = ligneShopify.getSku();
            String titre = ligneShopify.getTitle() != null ? ligneShopify.getTitle() : "Article";

            if (sku == null || sku.isBlank()) {
                throw new BadRequestException(
                        "Commande Shopify " + libelleCommande(payload) + " refusée : l'article \""
                                + titre + "\" n'a pas de SKU renseigné. Chaque produit Shopify doit "
                                + "avoir pour SKU le code de son produit correspondant dans Dioks (ex. DKS-00042).");
            }

            Produit produit = produitRepository.findByCode(sku).orElse(null);
            if (produit == null || produit.getVendeur() == null
                    || !produit.getVendeur().getId().equals(vendeur.getId())) {
                throw new BadRequestException(
                        "Commande Shopify " + libelleCommande(payload) + " refusée : le SKU \"" + sku
                                + "\" (article \"" + titre + "\") ne correspond à aucun produit de votre "
                                + "catalogue Dioks. Vérifiez le code produit dans Dioks et le SKU dans Shopify.");
            }

            int quantite = ligneShopify.getQuantity() != null && ligneShopify.getQuantity() > 0
                    ? ligneShopify.getQuantity() : 1;

            sn.votreplateforme.logistique.dto.LigneCommandeRequest ligne =
                    new sn.votreplateforme.logistique.dto.LigneCommandeRequest();
            ligne.setProduitId(produit.getId());
            ligne.setQuantite(quantite);
            lignes.add(ligne);
        }
        return lignes;
    }

    private String descriptionProduits(ShopifyOrderWebhook payload) {
        List<ShopifyLineItem> lignes = payload.getLineItems();
        if (lignes == null || lignes.isEmpty()) {
            return "Commande Shopify " + libelleCommande(payload);
        }
        return lignes.stream()
                .map(ligne -> (ligne.getQuantity() != null ? ligne.getQuantity() : 1)
                        + "x " + (ligne.getTitle() != null ? ligne.getTitle() : "Article"))
                .collect(Collectors.joining(", "));
    }

    private String libelleCommande(ShopifyOrderWebhook payload) {
        return payload.getName() != null ? payload.getName()
                : (payload.getOrderNumber() != null ? "#" + payload.getOrderNumber() : "");
    }

    /** Ne garde que le '+' initial et les chiffres (Shopify envoie parfois "555-123-4567" ou "(555) 123-4567"). */
    private String nettoyerTelephone(String telephone) {
        String nettoye = telephone.replaceAll("[^0-9+]", "");
        return nettoye.isBlank() ? telephone : nettoye;
    }

    private String premierNonVide(String... valeurs) {
        for (String v : valeurs) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
