package sn.votreplateforme.logistique.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sn.votreplateforme.logistique.entity.Livraison;

import java.math.BigDecimal;

/**
 * Service de notifications WhatsApp via Twilio
 * Gère l'envoi de notifications automatiques aux vendeurs et clients
 *
 * Documentation Twilio: https://www.twilio.com/docs/whatsapp/api
 */
@Service
@Slf4j
public class NotificationService {

    private boolean twilioEnabled = true;
    private String accountSid = "AC4dc6a916fcac4bd65bb132b70477d922";
    private String authToken = "7237a04ea01a490138f2ce7250fb0d4f";
    private String whatsappFrom = "whatsapp:+14155238886";
    private String adminPhone = "781082373"; // Format sans +221

    /**
     * Initialise le client Twilio au démarrage de l'application
     */
    @PostConstruct
    public void init() {
        if (twilioEnabled) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("✅ Twilio initialisé avec succès");
                log.info("📱 Numéro WhatsApp: {}", whatsappFrom);
            } catch (Exception e) {
                log.error("❌ Erreur lors de l'initialisation de Twilio", e);
                twilioEnabled = false;
            }
        } else {
            log.warn("⚠️ Twilio désactivé - Les notifications seront simulées");
        }
    }

    /**
     * Envoie un message WhatsApp via Twilio
     *
     * @param to Numéro du destinataire (format: 771234567 ou +221771234567)
     * @param messageText Contenu du message
     * @return true si envoyé avec succès, false sinon
     */
    public boolean envoyerWhatsApp(String to, String messageText) {
        try {

            System.out.println("formatte not : " + to);

            // Formater le numéro au format WhatsApp
            String formattedTo = formatPhoneNumber(to);

            System.out.println("formattedTo: " + formattedTo);

            // Envoyer le message via Twilio
            Message message = Message.creator(
                    new PhoneNumber(formattedTo),
                    new PhoneNumber(whatsappFrom),
                    messageText
            ).create();

            log.info("✅ WhatsApp envoyé - SID: {} - Destinataire: {}",
                    message.getSid(), formattedTo);

            return true;

        } catch (Exception e) {
            log.error("❌ Erreur envoi WhatsApp à {}: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Formate un numéro de téléphone sénégalais au format WhatsApp international
     *
     * @param phone Numéro brut (ex: 771234567, +221771234567)
     * @return Numéro formaté (ex: whatsapp:+221771234567)
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "";
        }

        // Nettoyer le numéro (supprimer espaces et tirets)
        String cleaned = phone.replaceAll("[\\s-]", "");

        // Si commence déjà par whatsapp:, retourner tel quel
        if (cleaned.startsWith("whatsapp:")) {
            return cleaned;
        }

        // Si commence par +221, ajouter juste whatsapp:
        if (cleaned.startsWith("+221")) {
            return "whatsapp:" + cleaned;
        }

        // Si commence par 221 (sans +), ajouter whatsapp:+
        if (cleaned.startsWith("221")) {
            return "whatsapp:+" + cleaned;
        }

        // Sinon, c'est un numéro sénégalais sans indicatif (77, 78, 70, 76, etc.)
        // Ajouter +221
        return "whatsapp:+221" + cleaned;
    }

    // ========================================
    // NOTIFICATIONS VENDEUR
    // ========================================

    /**
     * Notifie le vendeur qu'une nouvelle livraison a été créée
     */
    public void notifierNouvelleLivraison(Livraison livraison) {
        String message = String.format(
                "🚚 Nouvelle livraison créée !\n\n" +
                        "📦 Colis: #%s\n" +
                        "📍 Destination: %s, %s\n" +
                        "👤 Client: %s\n" +
                        "💰 Montant COD: %s FCFA\n\n" +
                        "Le ramassage sera effectué prochainement.",
                livraison.getNumeroTracking(),
                livraison.getAdresseDestination().getQuartier(),
                livraison.getAdresseDestination().getCommune(),
                livraison.getNomClient(),
                formatMontant(livraison.getMontantCOD())
        );

        envoyerWhatsApp(livraison.getVendeur().getTelephone(), message);
    }

    public String construireMessageRamassage(int nombreColis, String heureRamassage, String numerosSuivi) {
        if (nombreColis == 1) {
            return String.format(
                    "🚚 Ramassage prévu aujourd'hui vers %s.\n\n" +
                            "Préparez votre colis:\n%s\n\n" +
                            "Merci ! 📦",
                    heureRamassage,
                    numerosSuivi
            );
        } else {
            return String.format(
                    "🚚 Ramassage prévu aujourd'hui vers %s.\n\n" +
                            "Préparez vos %d colis:\n%s\n\n" +
                            "Merci ! 📦",
                    heureRamassage,
                    nombreColis,
                    numerosSuivi
            );
        }
    }

    /**
     * Notifie le vendeur que son colis a été ramassé
     */
    public void notifierRamassage(Livraison livraison) {
        String message = String.format(
                "✅ Colis ramassé !\n\n" +
                        "Bonjour %s,\n\n" +
                        "Votre colis #%s a été ramassé avec succès.\n" +
                        "Il est maintenant en traitement pour livraison.\n\n" +
                        "📍 Destination: %s, %s\n" +
                        "👤 Client: %s\n\n" +
                        "Merci de votre confiance ! 💚",
                livraison.getVendeur().getPrenom(),
                livraison.getNumeroTracking(),
                livraison.getAdresseDestination().getQuartier(),
                livraison.getAdresseDestination().getCommune(),
                livraison.getNomClient()
        );

        envoyerWhatsApp(livraison.getVendeur().getTelephone(), message);
    }

    /**
     * Notifie le vendeur que le livreur est en route
     */
    public void notifierEnRoute(Livraison livraison) {
        String message = String.format(
                "🚚 Livraison en cours !\n\n" +
                        "Bonjour %s,\n\n" +
                        "Votre colis #%s est en route vers le client.\n\n" +
                        "📍 Destination: %s\n" +
                        "💰 Montant à collecter: %s FCFA",
                livraison.getVendeur().getPrenom(),
                livraison.getNumeroTracking(),
                livraison.getAdresseDestination().getAdresseComplete(),
                formatMontant(livraison.getMontantCOD())
        );

        envoyerWhatsApp(livraison.getVendeur().getTelephone(), message);
    }

    /**
     * Notifie le vendeur que son colis a été livré avec succès
     */
    public void notifierLivraison(Livraison livraison) {
        BigDecimal montantARecevoir = livraison.getMontantCOD()
                .subtract(livraison.getFraisLivraison());

        String message = String.format(
                "🎉 Colis livré avec succès !\n\n" +
                        "Bonjour %s,\n\n" +
                        "Votre colis #%s a été livré !\n\n" +
                        "💰 Montant COD collecté: %s FCFA\n" +
                        "📊 Frais de livraison: %s FCFA\n" +
                        "💵 Montant à recevoir: %s FCFA\n\n" +
                        "Le montant sera disponible dans votre solde.\n" +
                        "Merci de votre confiance ! 💚",
                livraison.getVendeur().getPrenom(),
                livraison.getNumeroTracking(),
                formatMontant(livraison.getCashCollecte()),
                formatMontant(livraison.getFraisLivraison()),
                formatMontant(montantARecevoir)
        );

        envoyerWhatsApp(livraison.getVendeur().getTelephone(), message);
    }

    /**
     * Notifie le vendeur d'un échec de livraison
     */
    public void notifierEchecLivraison(Livraison livraison, String raison) {
        String message = String.format(
                "⚠️ Échec de livraison\n\n" +
                        "Bonjour %s,\n\n" +
                        "La livraison du colis #%s n'a pas pu être effectuée.\n\n" +
                        "Raison: %s\n\n" +
                        "Nous recontacterons le client pour reprogrammer la livraison.",
                livraison.getVendeur().getPrenom(),
                livraison.getNumeroTracking(),
                raison
        );

        envoyerWhatsApp(livraison.getVendeur().getTelephone(), message);
    }

    // ========================================
    // NOTIFICATIONS CLIENT
    // ========================================

    /**
     * Notifie le client que le livreur est en route
     */
    public void notifierClientEnRoute(Livraison livraison) {
        String message = String.format(
                "🚚 Votre colis arrive !\n\n" +
                        "Bonjour %s,\n\n" +
                        "Votre colis #%s est en route !\n" +
                        "Le livreur arrivera bientôt à votre adresse.\n\n" +
                        "💰 Montant à payer: %s FCFA\n" +
                        "📍 Adresse: %s\n\n" +
                        "Merci de préparer le montant exact 😊",
                livraison.getNomClient(),
                livraison.getNumeroTracking(),
                formatMontant(livraison.getMontantCOD()),
                livraison.getAdresseDestination().getAdresseComplete()
        );

        envoyerWhatsApp(livraison.getTelephoneClient(), message);
    }

    /**
     * Notifie le client que sa livraison est terminée
     */
    public void notifierClientLivraison(String telephone, String numeroTracking) {
        String message = String.format(
                "✅ Livraison effectuée !\n\n" +
                        "Votre colis #%s a été livré avec succès !\n\n" +
                        "Merci pour votre achat ! 🎉\n" +
                        "À bientôt ! 😊",
                numeroTracking
        );

        envoyerWhatsApp(telephone, message);
    }

    // ========================================
    // NOTIFICATIONS ADMIN
    // ========================================

    /**
     * Envoie une notification à l'admin
     */
    public void envoyerNotificationAdmin(String titre, String message) {
        if (adminPhone == null || adminPhone.isEmpty()) {
            log.warn("⚠️ Numéro admin non configuré - Notification ignorée");
            return;
        }

        String messageComplet = String.format("🔔 %s\n\n%s", titre, message);
        envoyerWhatsApp(adminPhone, messageComplet);
    }

    /**
     * Notifie l'admin d'une nouvelle demande de vendeur
     */
    public void notifierNouveauVendeur(String nomVendeur, String telephone) {
        String message = String.format(
                "Nouvelle demande vendeur :\n\n" +
                        "👤 Nom: %s\n" +
                        "📱 Téléphone: %s\n\n" +
                        "En attente de validation.",
                nomVendeur,
                telephone
        );

        envoyerNotificationAdmin("Nouveau vendeur", message);
    }

    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================

    /**
     * Envoie une notification au vendeur (générique)
     */
    public void envoyerNotificationVendeur(String telephone, String titre, String message) {
        String messageComplet = String.format("📱 %s\n\n%s", titre, message);
        envoyerWhatsApp(telephone, messageComplet);
    }

    /**
     * Formate un montant en FCFA avec séparateurs de milliers
     *
     * @param montant Montant à formater
     * @return Montant formaté (ex: "35 000")
     */
    private String formatMontant(BigDecimal montant) {
        if (montant == null) {
            return "0";
        }
        return String.format("%,d", montant.longValue()).replace(',', ' ');
    }
}