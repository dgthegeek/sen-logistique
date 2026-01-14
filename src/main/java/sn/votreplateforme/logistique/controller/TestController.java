package sn.votreplateforme.logistique.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.service.NotificationService;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/whatsapp")
    public ResponseEntity<String> testWhatsApp() {
        notificationService.envoyerNotificationVendeur(
                "+212676336628",
                "🎉 Test Twilio",
                "Si tu reçois ce message, l'intégration fonctionne parfaitement ! 💚"
        );
        return ResponseEntity.ok("Message envoyé ! Vérifie ton WhatsApp 📱");
    }
}