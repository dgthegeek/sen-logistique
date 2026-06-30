package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.exception.BusinessException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Delivery - Gestion de la confirmation de livraison
 *
 * Responsabilités :
 * - Récupérer les infos de livraison après scan QR
 * - Confirmer la livraison et mettre à jour le statut
 * - Enregistrer le cash collecté
 * - Déclencher les notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryService {

    private final LivraisonRepository livraisonRepository;
    private final NotificationService notificationService;
    private final StockService stockService;

    /**
     * Récupère les informations de livraison pour le formulaire de confirmation
     *
     * @param numeroTracking Numéro de tracking
     * @return DeliveryInfoResponse avec toutes les infos pré-remplies
     */
    @Transactional(readOnly = true)
    public DeliveryInfoResponse getDeliveryInfo(String numeroTracking) {
        log.info("Récupération infos delivery pour: {}", numeroTracking);

        // 1. Recuperer la livraison
        Livraison livraison = livraisonRepository.findByNumeroTracking(numeroTracking)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Colis non trouvée: " + numeroTracking
                ));

        // 2. Verifier que le colis est au bon statut (RAMASSE)
        if (livraison.getStatut() != StatutLivraison.RAMASSE) {
            log.warn("Tentative de livraison d'un colis pas au statut RAMASSE: {} - Statut actuel: {}",
                    numeroTracking, livraison.getStatut());
            throw new ResourceNotFoundException("Colis non trouvée: " + numeroTracking);
        }

        log.debug("Livraison: {}", livraison.getStatut());

        // 3. Construire le DTO selon le schéma OpenAPI
        DeliveryInfoResponse response = new DeliveryInfoResponse();
        response.setNumeroTracking(numeroTracking);
        response.setMontantACollecter(livraison.getMontantCOD());

        // Client (objet imbriqué)
        DeliveryInfoResponseClient client = new DeliveryInfoResponseClient();
        client.setNom(livraison.getNomClient());
        client.setTelephone(livraison.getTelephoneClient());

        // Adresse complète formatée
        String adresseComplete = String.format("%s, %s",
                livraison.getAdresseDestination().getQuartier(),
                livraison.getAdresseDestination().getAdresseComplete()
        );
        client.setAdresse(adresseComplete);
        client.setPointRepere(livraison.getAdresseDestination().getPointRepere());
        response.setClient(client);

        // Produit (objet imbriqué)
        DeliveryInfoResponseProduit produit = new DeliveryInfoResponseProduit();
        produit.setDescription(livraison.getDescriptionProduit());
        produit.setFragile(livraison.getFragile());
        response.setProduit(produit);

        // Vendeur (objet imbriqué)
        DeliveryInfoResponseVendeur vendeur = new DeliveryInfoResponseVendeur();
        vendeur.setNom(livraison.getVendeur().getPrenom()); // Prénom du vendeur
        response.setVendeur(vendeur);

        log.info("Infos delivery {} récupérées - Client: {}, Montant: {} FCFA",
                numeroTracking,
                livraison.getNomClient(),
                livraison.getMontantCOD()
        );

        return response;
    }

    /**
     * Confirme la livraison du colis
     *
     * Change le statut : RAMASSE → LIVREE
     * Enregistre le cash collecté
     * Met à jour les dates
     * Envoie les notifications
     *
     * @param numeroTracking Numéro de tracking
     * @param request Données de confirmation
     * @return Message de confirmation
     */
    @Transactional
    public Map<String, Object> confirmerLivraison(String numeroTracking, ConfirmLivraisonRequest request) {
        log.info("Confirmation livraison: {} - Cash collecté: {} FCFA",
                numeroTracking,
                request.getCashCollecte()
        );

        // 1. Récupérer la livraison
        Livraison livraison = livraisonRepository.findByNumeroTracking(numeroTracking)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Colis non trouvé: " + numeroTracking
                ));

        // 2. Vérifier le statut actuel
        if (livraison.getStatut() != StatutLivraison.RAMASSE) {
            throw new BusinessException(
                    String.format(
                            "Le colis %s ne peut pas être livré. Statut actuel: %s (attendu: RAMASSE)",
                            numeroTracking,
                            livraison.getStatut()
                    )
            );
        }

        // 3. Vérifier que le colis a été remis
        if (!request.getColisRemis()) {
            throw new BusinessException(
                    "Le colis doit être marqué comme remis pour confirmer la livraison"
            );
        }

        // 4. Vérifier le montant collecté
        if (request.getCashCollecte().compareTo(livraison.getMontantCOD()) != 0) {
            log.warn("⚠️ Montant collecté ({}) différent du montant COD attendu ({}) pour {}",
                    request.getCashCollecte(),
                    livraison.getMontantCOD(),
                    numeroTracking
            );
            // On accepte quand même mais on log
        }

        // 5. Mettre à jour la livraison
        livraison.setStatut(StatutLivraison.LIVREE);
        livraison.setDateLivraison(LocalDateTime.now());
        livraison.setCashCollecte(request.getCashCollecte());
        livraison.setCommentaireLivraison(request.getCommentaire());

        livraisonRepository.save(livraison);

        // Décrément automatique du stock (multi-produits ou produit unique)
        stockService.enregistrerSortiesLivraison(livraison);

        log.info("✅ Livraison {} mise à jour - Statut: LIVREE", numeroTracking);

        // 6. Calculer le montant que le vendeur va recevoir
        BigDecimal montantVendeur = livraison.getMontantCOD()
                .subtract(livraison.getFraisLivraison());

        // 7. Mettre à jour le solde du vendeur
        livraison.getVendeur().setSoldeEnAttente(
                livraison.getVendeur().getSoldeEnAttente().add(montantVendeur)
        );

        log.info("💰 Solde vendeur {} mis à jour: +{} FCFA",
                livraison.getVendeur().getPrenom(),
                montantVendeur
        );

        // 8. Envoyer les notifications
        try {
            // 1️⃣ NOTIFICATION AU VENDEUR
            notificationService.notifierLivraison(livraison);

            // 2️⃣ NOTIFICATION AU CLIENT
            notificationService.notifierClientLivraison(
                    livraison.getTelephoneClient(),
                    numeroTracking
            );

            log.info("📱 Notifications envoyées avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi des notifications: {}", e.getMessage());
            // On ne bloque pas si les notifications échouent
        }

        // 9. Retourner la réponse (selon le schéma OpenAPI)
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Livraison confirmée avec succès");
        response.put("numeroTracking", numeroTracking);

        log.info("🎉 Livraison {} confirmée avec succès !", numeroTracking);

        return response;
    }
}