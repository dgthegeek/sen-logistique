package sn.votreplateforme.logistique.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.votreplateforme.logistique.entity.Livraison;

import java.math.BigDecimal;

/**
 * Service de notifications WhatsApp
 * Utilise Twilio WhatsApp API pour envoyer des messages
 *
 * Note: Pour l'instant, les notifications sont simulÃ©es (logs uniquement)
 * L'intÃ©gration rÃ©elle avec Twilio sera faite plus tard
 */
@Service
@Slf4j
public class NotificationService {

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String whatsappFrom;

    /**
     * Envoie une notification WhatsApp Ã  un vendeur pour le ramassage
     *
     * @param telephoneVendeur NumÃ©ro du vendeur (format: 771234567)
     * @param message Message Ã  envoyer
     */
    public void envoyerNotificationRamassage(String telephoneVendeur, String message) {
        log.info("ðŸ“± Notification ramassage - Destinataire: {}", telephoneVendeur);

        if (!twilioEnabled) {
            log.warn("âš ï¸ Twilio dÃ©sactivÃ© - Simulation d'envoi WhatsApp");
            log.info("ðŸ“© Message simulÃ©: {}", message);
            return;
        }

        // TODO: ImplÃ©menter l'envoi rÃ©el via Twilio
        String to = "whatsapp:+221" + telephoneVendeur;

        log.info("ðŸ“¤ Envoi WhatsApp Ã : {}", to);
        log.debug("Message: {}", message);

        // Code d'intÃ©gration Twilio Ã  ajouter ici
        // Message twilioMessage = Message.creator(
        //     new PhoneNumber(to),
        //     new PhoneNumber(whatsappFrom),
        //     message
        // ).create();

        log.info("âœ… Notification envoyÃ©e (simulation)");
    }

    /**
     * Envoie une notification WhatsApp au client final
     *
     * @param telephoneClient NumÃ©ro du client
     * @param message Message Ã  envoyer
     */
    public void envoyerNotificationClient(String telephoneClient, String message) {
        log.info("ðŸ“± Notification client - Destinataire: {}", telephoneClient);

        if (!twilioEnabled) {
            log.warn("âš ï¸ Twilio dÃ©sactivÃ© - Simulation d'envoi WhatsApp");
            log.info("ðŸ“© Message simulÃ©: {}", message);
            return;
        }

        String to = "whatsapp:+221" + telephoneClient;

        log.info("ðŸ“¤ Envoi WhatsApp Ã : {}", to);
        log.debug("Message: {}", message);

        // TODO: ImplÃ©menter l'envoi rÃ©el via Twilio

        log.info("âœ… Notification envoyÃ©e (simulation)");
    }

    /**
     * Construit le message de notification pour ramassage groupÃ©
     *
     * @param nombreColis Nombre de colis Ã  ramasser
     * @param heureRamassage Heure prÃ©vue (ex: "15h")
     * @param numerosSuivi Liste des numÃ©ros de suivi
     * @return Message formatÃ©
     */
    public String construireMessageRamassage(int nombreColis, String heureRamassage, String numerosSuivi) {
        if (nombreColis == 1) {
            return String.format(
                    "ðŸšš Ramassage prÃ©vu aujourd'hui vers %s.\n\n" +
                            "PrÃ©parez votre colis:\n%s\n\n" +
                            "Merci ! ðŸ“¦",
                    heureRamassage,
                    numerosSuivi
            );
        } else {
            return String.format(
                    "ðŸšš Ramassage prÃ©vu aujourd'hui vers %s.\n\n" +
                            "PrÃ©parez vos %d colis:\n%s\n\n" +
                            "Merci ! ðŸ“¦",
                    heureRamassage,
                    nombreColis,
                    numerosSuivi
            );
        }
    }

    /**
     * Construit le message de notification aprÃ¨s ramassage
     *
     * @param numeroTracking NumÃ©ro de suivi
     * @return Message formatÃ©
     */
    public String construireMessageApresRamassage(String numeroTracking) {
        return String.format(
                "âœ… Votre colis %s a Ã©tÃ© rÃ©cupÃ©rÃ© avec succÃ¨s !\n\n" +
                        "Il sera livrÃ© dans les prochaines 24-48h.\n\n" +
                        "Merci de votre confiance ! ðŸ’š",
                numeroTracking
        );
    }

    /**
     * Construit le message de notification pour le client aprÃ¨s ramassage
     *
     * @param numeroTracking NumÃ©ro de suivi
     * @param urlTracking URL de suivi
     * @return Message formatÃ©
     */
    public String construireMessageClientRamassage(String numeroTracking, String urlTracking) {
        return String.format(
                "ðŸ“¦ Votre colis %s a Ã©tÃ© rÃ©cupÃ©rÃ© !\n\n" +
                        "Livraison prÃ©vue demain.\n\n" +
                        "Suivez votre colis: %s\n\n" +
                        "Ã€ bientÃ´t ! ðŸ˜Š",
                numeroTracking,
                urlTracking
        );
    }

    /**
     * Construit le message de confirmation de livraison
     *
     * @param numeroTracking NumÃ©ro de suivi
     * @return Message formatÃ©
     */
    public String construireMessageLivraison(String numeroTracking) {
        return String.format(
                "âœ… Votre colis %s a Ã©tÃ© livrÃ© avec succÃ¨s !\n\n" +
                        "Merci pour votre achat ! ðŸŽ‰\n\n" +
                        "Ã€ bientÃ´t ! ðŸ˜Š",
                numeroTracking
        );
    }

    // MÃ‰THODES Ã€ AJOUTER DANS NotificationService.java

    /**
     * Construit le message de confirmation de livraison pour le vendeur
     *
     * @param numeroTracking NumÃ©ro de suivi
     * @param montantARecevoir Montant que le vendeur va recevoir
     * @return Message formatÃ©
     */
    public String construireMessageLivraisonVendeur(String numeroTracking, java.math.BigDecimal montantARecevoir) {
        return String.format(
                "âœ… Votre colis %s a Ã©tÃ© livrÃ© avec succÃ¨s !\n\n" +
                        "Vous recevrez %,.0f FCFA dans votre prochain paiement.\n\n" +
                        "Merci de votre confiance ! ðŸ’š",
                numeroTracking,
                montantARecevoir
        );
    }

    // AJOUTS Ã€ FAIRE DANS NotificationService.java
// (Ajouter ces mÃ©thodes Ã  la fin de ton NotificationService existant)

    /**
     * Envoie une notification au vendeur aprÃ¨s livraison rÃ©ussie
     *
     * @param telephone TÃ©lÃ©phone du vendeur
     * @param numeroTracking NumÃ©ro de tracking
     * @param montantARecevoir Montant que le vendeur va recevoir
     */
    public void envoyerNotificationVendeurLivraison(
            String telephone,
            String numeroTracking,
            BigDecimal montantARecevoir
    ) {
        String message = String.format(
                "âœ… Votre colis %s a Ã©tÃ© livrÃ© avec succÃ¨s !\n\n" +
                        "Vous recevrez %s FCFA lors du prochain paiement.\n\n" +
                        "Merci de votre confiance ! ðŸ’š",
                numeroTracking,
                formatMontant(montantARecevoir)
        );

        //envoyerWhatsApp(telephone, message);

        log.info("ðŸ“± Notification vendeur (livraison) envoyÃ©e: {} - {}",
                telephone, numeroTracking);
    }

    /**
     * Envoie une notification au client aprÃ¨s livraison rÃ©ussie
     *
     * @param telephone TÃ©lÃ©phone du client
     * @param numeroTracking NumÃ©ro de tracking
     */
    public void envoyerNotificationClientLivraison(
            String telephone,
            String numeroTracking
    ) {
        String message = String.format(
                "âœ… Votre colis %s a Ã©tÃ© livrÃ© avec succÃ¨s !\n\n" +
                        "Merci pour votre achat ! ðŸŽ‰\n\n" +
                        "Ã€ bientÃ´t ! ðŸ˜Š",
                numeroTracking
        );

        //envoyerWhatsApp(telephone, message);

        log.info("ðŸ“± Notification client (livraison) envoyÃ©e: {} - {}",
                telephone, numeroTracking);
    }

    /**
     * Formate un montant en FCFA avec sÃ©parateurs de milliers
     *
     * @param montant Montant Ã  formater
     * @return Montant formatÃ© (ex: "35,000")
     */
    private String formatMontant(BigDecimal montant) {
        return String.format("%,d", montant.longValue()).replace(',', ' ');


    }

    /**
     * Envoie une notification au vendeur
     */
    public void envoyerNotificationVendeur(String telephone, String titre, String message) {
        log.info("📱 [SIMULATION] Notification WhatsApp au vendeur: {}", telephone);
        log.info("   Titre: {}", titre);
        log.info("   Message: {}", message);

        // TODO: Implémenter avec Twilio WhatsApp API en production
        // Example:
        // twilioClient.messages.create(
        //     to: "whatsapp:" + telephone,
        //     from: "whatsapp:+221XXXXXXXXX",
        //     body: titre + "\n\n" + message
        // )
    }

    /**
     * Envoie une notification à l'admin
     */
    public void envoyerNotificationAdmin(String titre, String message) {
        log.info("📱 [SIMULATION] Notification WhatsApp à l'admin");
        log.info("   Titre: {}", titre);
        log.info("   Message: {}", message);

        // TODO: Implémenter avec Twilio WhatsApp API en production
        // Envoyer au numéro de l'admin configuré dans application.yml
    }

    /**
     * Notifie le vendeur que son colis a été ramassé
     */
    public void notifierRamassage(Livraison livraison) {
        String titre = "✅ Colis ramassé !";
        String message = String.format(
                "Bonjour %s,\n\n" +
                        "Votre colis #%s a été ramassé avec succès.\n" +
                        "Il est maintenant en traitement pour livraison.\n\n" +
                        "Destination: %s, %s\n" +
                        "Client: %s",
                livraison.getVendeur().getPrenom(),
                livraison.getNumeroTracking(),
                livraison.getAdresseDestination().getQuartier(),
                livraison.getAdresseDestination().getCommune(),
                livraison.getNomClient()
        );

        envoyerNotificationVendeur(livraison.getVendeur().getTelephone(), titre, message);
    }

    /**
     * Notifie le vendeur que son colis a été livré
     */
    public void notifierLivraison(Livraison livraison) {
        String titre = "🎉 Colis livré !";
        String montantARecevoir = livraison.getMontantCOD()
                .subtract(livraison.getFraisLivraison())
                .toString();

        String message = String.format(
                "Bonjour %s,\n\n" +
                        "Votre colis #%s a été livré avec succès !\n\n" +
                        "Montant COD collecté: %s FCFA\n" +
                        "Frais de livraison: %s FCFA\n" +
                        "Montant à recevoir: %s FCFA\n\n" +
                        "Le montant sera disponible dans votre solde.",
                livraison.getVendeur().getPrenom(),
                livraison.getNumeroTracking(),
                livraison.getCashCollecte().toString(),
                livraison.getFraisLivraison().toString(),
                montantARecevoir
        );

        envoyerNotificationVendeur(livraison.getVendeur().getTelephone(), titre, message);
    }

    /**
     * Notifie le client qu'un livreur est en route
     */
    public void notifierClientEnRoute(Livraison livraison) {
        String titre = "🚚 Livraison en route";
        String message = String.format(
                "Bonjour %s,\n\n" +
                        "Votre colis #%s est en route !\n" +
                        "Le livreur arrivera bientôt.\n\n" +
                        "Montant à payer: %s FCFA\n" +
                        "Adresse: %s",
                livraison.getNomClient(),
                livraison.getNumeroTracking(),
                livraison.getMontantCOD().toString(),
                livraison.getAdresseDestination().getAdresseComplete()
        );

        envoyerNotificationVendeur(livraison.getTelephoneClient(), titre, message);
    }
}
