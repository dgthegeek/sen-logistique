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
 * ResponsabilitÃ©s :
 * - RÃ©cupÃ©rer les infos de livraison aprÃ¨s scan QR
 * - Confirmer la livraison et mettre Ã  jour le statut
 * - Enregistrer le cash collectÃ©
 * - DÃ©clencher les notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryService {

    private final LivraisonRepository livraisonRepository;
    private final NotificationService notificationService;

    /**
     * RÃ©cupÃ¨re les informations de livraison pour le formulaire de confirmation
     *
     * @param numeroTracking NumÃ©ro de tracking
     * @return DeliveryInfoResponse avec toutes les infos prÃ©-remplies
     */
    @Transactional(readOnly = true)
    public DeliveryInfoResponse getDeliveryInfo(String numeroTracking) {
        log.info("RÃ©cupÃ©ration infos delivery pour: {}", numeroTracking);

        // 1. Recuperer la livraison
        Livraison livraison = livraisonRepository.findByNumeroTracking(numeroTracking)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Colis non trouvee: " + numeroTracking
                ));

        // 2. Verifier que le colis est au bon statut (RAMASSE)
        if (livraison.getStatut() != StatutLivraison.RAMASSE) {
            log.warn("Tentative de livraison d'un colis pas au statut RAMASSE: {} - Statut actuel: {}",
                    numeroTracking, livraison.getStatut());
            throw new ResourceNotFoundException("Colis non trouvee: " + numeroTracking);
        }

        System.out.println("livraison: "+livraison.getStatut());

        // 3. Construire le DTO selon le schÃ©ma OpenAPI
        DeliveryInfoResponse response = new DeliveryInfoResponse();
        response.setNumeroTracking(numeroTracking);
        response.setMontantACollecter(livraison.getMontantCOD());

        // Client (objet imbriquÃ©)
        DeliveryInfoResponseClient client = new DeliveryInfoResponseClient();
        client.setNom(livraison.getNomClient());
        client.setTelephone(livraison.getTelephoneClient());

        // Adresse complÃ¨te formatÃ©e
        String adresseComplete = String.format("%s, %s",
                livraison.getAdresseDestination().getQuartier(),
                livraison.getAdresseDestination().getAdresseComplete()
        );
        client.setAdresse(adresseComplete);
        client.setPointRepere(livraison.getAdresseDestination().getPointRepere());
        response.setClient(client);

        // Produit (objet imbriquÃ©)
        DeliveryInfoResponseProduit produit = new DeliveryInfoResponseProduit();
        produit.setDescription(livraison.getDescriptionProduit());
        produit.setFragile(livraison.getFragile());
        response.setProduit(produit);

        // Vendeur (objet imbriquÃ©)
        DeliveryInfoResponseVendeur vendeur = new DeliveryInfoResponseVendeur();
        vendeur.setNom(livraison.getVendeur().getPrenom()); // PrÃ©nom du vendeur
        response.setVendeur(vendeur);

        log.info("Infos delivery {} rÃ©cupÃ©rÃ©es - Client: {}, Montant: {} FCFA",
                numeroTracking,
                livraison.getNomClient(),
                livraison.getMontantCOD()
        );

        return response;
    }

    /**
     * Confirme la livraison du colis
     *
     * Change le statut : RAMASSE â†’ LIVREE
     * Enregistre le cash collectÃ©
     * Met Ã  jour les dates
     * Envoie les notifications
     *
     * @param numeroTracking NumÃ©ro de tracking
     * @param request DonnÃ©es de confirmation
     * @return Message de confirmation
     */
    @Transactional
    public Map<String, Object> confirmerLivraison(String numeroTracking, ConfirmLivraisonRequest request) {
        log.info("Confirmation livraison: {} - Cash collectÃ©: {} FCFA",
                numeroTracking,
                request.getCashCollecte()
        );

        // 1. RÃ©cupÃ©rer la livraison
        Livraison livraison = livraisonRepository.findByNumeroTracking(numeroTracking)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Colis non trouvÃ©: " + numeroTracking
                ));

        // 2. VÃ©rifier le statut actuel
        if (livraison.getStatut() != StatutLivraison.RAMASSE) {
            throw new BusinessException(
                    String.format(
                            "Le colis %s ne peut pas Ãªtre livrÃ©. Statut actuel: %s (attendu: RAMASSE)",
                            numeroTracking,
                            livraison.getStatut()
                    )
            );
        }

        // 3. VÃ©rifier que le colis a Ã©tÃ© remis
        if (!request.getColisRemis()) {
            throw new BusinessException(
                    "Le colis doit Ãªtre marquÃ© comme remis pour confirmer la livraison"
            );
        }

        // 4. VÃ©rifier le montant collectÃ©
        if (request.getCashCollecte().compareTo(livraison.getMontantCOD()) != 0) {
            log.warn("âš ï¸ Montant collectÃ© ({}) diffÃ©rent du montant COD attendu ({}) pour {}",
                    request.getCashCollecte(),
                    livraison.getMontantCOD(),
                    numeroTracking
            );
            // On accepte quand mÃªme mais on log
        }

        // 5. Mettre Ã  jour la livraison
        livraison.setStatut(StatutLivraison.LIVREE);
        livraison.setDateLivraison(LocalDateTime.now());
        livraison.setCashCollecte(request.getCashCollecte());
        livraison.setCommentaireLivraison(request.getCommentaire());

        livraisonRepository.save(livraison);

        log.info("âœ… Livraison {} mise Ã  jour - Statut: LIVREE", numeroTracking);

        // 6. Calculer le montant que le vendeur va recevoir
        BigDecimal montantVendeur = livraison.getMontantCOD()
                .subtract(livraison.getFraisLivraison());

        // 7. Mettre Ã  jour le solde du vendeur
        livraison.getVendeur().setSoldeEnAttente(
                livraison.getVendeur().getSoldeEnAttente().add(montantVendeur)
        );

        log.info("ðŸ’° Solde vendeur {} mis Ã  jour: +{} FCFA",
                livraison.getVendeur().getPrenom(),
                montantVendeur
        );

        // 8. Envoyer les notifications
        try {
            // Notification au vendeur
            notificationService.envoyerNotificationVendeurLivraison(
                    livraison.getVendeur().getTelephone(),
                    numeroTracking,
                    montantVendeur
            );

            // Notification au client
            notificationService.envoyerNotificationClientLivraison(
                    livraison.getTelephoneClient(),
                    numeroTracking
            );

            log.info("ðŸ“± Notifications envoyÃ©es avec succÃ¨s");
        } catch (Exception e) {
            log.error("âŒ Erreur lors de l'envoi des notifications: {}", e.getMessage());
            // On ne bloque pas si les notifications Ã©chouent
        }

        // 9. Retourner la rÃ©ponse (selon le schÃ©ma OpenAPI)
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Livraison confirmÃ©e avec succÃ¨s");
        response.put("numeroTracking", numeroTracking);

        log.info("ðŸŽ‰ Livraison {} confirmÃ©e avec succÃ¨s !", numeroTracking);

        return response;
    }
}