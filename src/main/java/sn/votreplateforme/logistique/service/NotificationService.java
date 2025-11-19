package sn.votreplateforme.logistique.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service de notifications WhatsApp
 * Utilise Twilio WhatsApp API pour envoyer des messages
 * 
 * Note: Pour l'instant, les notifications sont simulées (logs uniquement)
 * L'intégration réelle avec Twilio sera faite plus tard
 */
@Service
@Slf4j
public class NotificationService {
    
    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;
    
    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String whatsappFrom;
    
    /**
     * Envoie une notification WhatsApp à un vendeur pour le ramassage
     * 
     * @param telephoneVendeur Numéro du vendeur (format: 771234567)
     * @param message Message à envoyer
     */
    public void envoyerNotificationRamassage(String telephoneVendeur, String message) {
        log.info("📱 Notification ramassage - Destinataire: {}", telephoneVendeur);
        
        if (!twilioEnabled) {
            log.warn("⚠️ Twilio désactivé - Simulation d'envoi WhatsApp");
            log.info("📩 Message simulé: {}", message);
            return;
        }
        
        // TODO: Implémenter l'envoi réel via Twilio
        String to = "whatsapp:+221" + telephoneVendeur;
        
        log.info("📤 Envoi WhatsApp à: {}", to);
        log.debug("Message: {}", message);
        
        // Code d'intégration Twilio à ajouter ici
        // Message twilioMessage = Message.creator(
        //     new PhoneNumber(to),
        //     new PhoneNumber(whatsappFrom),
        //     message
        // ).create();
        
        log.info("✅ Notification envoyée (simulation)");
    }
    
    /**
     * Envoie une notification WhatsApp au client final
     * 
     * @param telephoneClient Numéro du client
     * @param message Message à envoyer
     */
    public void envoyerNotificationClient(String telephoneClient, String message) {
        log.info("📱 Notification client - Destinataire: {}", telephoneClient);
        
        if (!twilioEnabled) {
            log.warn("⚠️ Twilio désactivé - Simulation d'envoi WhatsApp");
            log.info("📩 Message simulé: {}", message);
            return;
        }
        
        String to = "whatsapp:+221" + telephoneClient;
        
        log.info("📤 Envoi WhatsApp à: {}", to);
        log.debug("Message: {}", message);
        
        // TODO: Implémenter l'envoi réel via Twilio
        
        log.info("✅ Notification envoyée (simulation)");
    }
    
    /**
     * Construit le message de notification pour ramassage groupé
     * 
     * @param nombreColis Nombre de colis à ramasser
     * @param heureRamassage Heure prévue (ex: "15h")
     * @param numerosSuivi Liste des numéros de suivi
     * @return Message formaté
     */
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
     * Construit le message de notification après ramassage
     * 
     * @param numeroTracking Numéro de suivi
     * @return Message formaté
     */
    public String construireMessageApresRamassage(String numeroTracking) {
        return String.format(
            "✅ Votre colis %s a été récupéré avec succès !\n\n" +
            "Il sera livré dans les prochaines 24-48h.\n\n" +
            "Merci de votre confiance ! 💚",
            numeroTracking
        );
    }
    
    /**
     * Construit le message de notification pour le client après ramassage
     * 
     * @param numeroTracking Numéro de suivi
     * @param urlTracking URL de suivi
     * @return Message formaté
     */
    public String construireMessageClientRamassage(String numeroTracking, String urlTracking) {
        return String.format(
            "📦 Votre colis %s a été récupéré !\n\n" +
            "Livraison prévue demain.\n\n" +
            "Suivez votre colis: %s\n\n" +
            "À bientôt ! 😊",
            numeroTracking,
            urlTracking
        );
    }
    
    /**
     * Construit le message de confirmation de livraison
     * 
     * @param numeroTracking Numéro de suivi
     * @return Message formaté
     */
    public String construireMessageLivraison(String numeroTracking) {
        return String.format(
            "✅ Votre colis %s a été livré avec succès !\n\n" +
            "Merci pour votre achat ! 🎉\n\n" +
            "À bientôt ! 😊",
            numeroTracking
        );
    }
}
