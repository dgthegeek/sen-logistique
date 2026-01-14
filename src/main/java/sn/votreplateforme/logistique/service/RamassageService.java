package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.repository.LivraisonRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de gestion des ramassages
 * Gère les ramassages groupés par zone et les notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RamassageService {

    private final LivraisonRepository livraisonRepository;
    private final NotificationService notificationService;
    private final QRCodeService qrCodeService;

    /**
     * Récupère tous les ramassages à faire, groupés par zone
     */
    @Transactional(readOnly = true)
    public Map<String, List<Livraison>> getRamassagesGroupesParZone() {
        log.info("=== Récupération des ramassages groupés par zone ===");

        // Récupérer toutes les livraisons en attente de ramassage
        List<Livraison> livraisonsEnAttente = livraisonRepository
                .findByStatut(StatutLivraison.EN_ATTENTE_RAMASSAGE);

        log.info("Nombre de colis en attente: {}", livraisonsEnAttente.size());

        // Grouper par zone
        Map<String, List<Livraison>> ramassagesParZone = livraisonsEnAttente.stream()
                .collect(Collectors.groupingBy(
                        livraison -> livraison.getAdresseDestination().getZone().getNom()
                ));

        log.info("Zones avec ramassages: {}", ramassagesParZone.keySet());

        return ramassagesParZone;
    }

    /**
     * Récupère les ramassages d'aujourd'hui, groupés par zone
     */
    @Transactional(readOnly = true)
    public Map<String, List<Livraison>> getRamassagesAujourdhui() {
        log.info("=== Récupération des ramassages du jour ===");

        LocalDateTime debutJournee = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime finJournee = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        // Récupérer les livraisons créées aujourd'hui en attente de ramassage
        List<Livraison> livraisonsAujourdhui = livraisonRepository
                .findByStatut(StatutLivraison.EN_ATTENTE_RAMASSAGE)
                .stream()
                .filter(l -> l.getDateCreation().isAfter(debutJournee)
                        && l.getDateCreation().isBefore(finJournee))
                .collect(Collectors.toList());

        log.info("Nombre de colis à ramasser aujourd'hui: {}", livraisonsAujourdhui.size());

        // Grouper par zone
        Map<String, List<Livraison>> ramassagesParZone = livraisonsAujourdhui.stream()
                .collect(Collectors.groupingBy(
                        livraison -> livraison.getAdresseDestination().getZone().getNom()
                ));

        return ramassagesParZone;
    }

    /**
     * Notifie les vendeurs d'une zone pour préparer leurs colis
     *
     * @param zoneNom Nom de la zone
     * @param heureRamassage Heure prévue du ramassage (ex: "15h")
     * @return Nombre de vendeurs notifiés
     */
    @Transactional
    public int notifierVendeursZone(String zoneNom, String heureRamassage) {
        log.info("=== Notification vendeurs zone: {} à {} ===", zoneNom, heureRamassage);

        // Récupérer les livraisons de la zone en attente
        List<Livraison> livraisonsZone = livraisonRepository
                .findByStatut(StatutLivraison.EN_ATTENTE_RAMASSAGE)
                .stream()
                .filter(l -> l.getAdresseDestination().getZone().getNom().equals(zoneNom))
                .collect(Collectors.toList());

        if (livraisonsZone.isEmpty()) {
            log.warn("Aucune livraison en attente pour la zone: {}", zoneNom);
            return 0;
        }

        // Grouper par vendeur
        Map<Vendeur, List<Livraison>> livraisonsParVendeur = livraisonsZone.stream()
                .collect(Collectors.groupingBy(Livraison::getVendeur));

        log.info("Nombre de vendeurs à notifier: {}", livraisonsParVendeur.size());

        // Notifier chaque vendeur
        int vendeursNotifies = 0;
        for (Map.Entry<Vendeur, List<Livraison>> entry : livraisonsParVendeur.entrySet()) {
            Vendeur vendeur = entry.getKey();
            List<Livraison> livraisons = entry.getValue();

            // Construire la liste des numéros de tracking
            String numerosSuivi = livraisons.stream()
                    .map(Livraison::getNumeroTracking)
                    .map(num -> "• " + num)
                    .collect(Collectors.joining("\n"));

            // Construire et envoyer le message
            String message = notificationService.construireMessageRamassage(
                    livraisons.size(),
                    heureRamassage,
                    numerosSuivi
            );

            notificationService.envoyerWhatsApp(
                    vendeur.getTelephone(),
                    message
            );

            vendeursNotifies++;

            log.debug("Vendeur notifié: {} {} - {} colis",
                    vendeur.getPrenom(), vendeur.getNom(), livraisons.size());
        }

        log.info("✅ {} vendeurs notifiés pour la zone {}", vendeursNotifies, zoneNom);

        return vendeursNotifies;
    }

    /**
     * Marque des colis comme ramassés et envoie les notifications
     *
     * @param livraisonIds Liste des IDs de livraisons
     * @return Nombre de colis marqués comme ramassés
     */
    @Transactional
    public int marquerCommeRamasse(List<Long> livraisonIds) {
        log.info("=== Marquage de {} colis comme ramassés ===", livraisonIds.size());

        int colisRamasses = 0;

        for (Long livraisonId : livraisonIds) {
            Optional<Livraison> optLivraison = livraisonRepository.findById(livraisonId);

            if (optLivraison.isEmpty()) {
                log.warn("Livraison {} non trouvée", livraisonId);
                continue;
            }

            Livraison livraison = optLivraison.get();

            // Vérifier que le statut est bien EN_ATTENTE_RAMASSAGE
            if (livraison.getStatut() != StatutLivraison.EN_ATTENTE_RAMASSAGE) {
                log.warn("Livraison {} n'est pas en attente de ramassage (statut: {})",
                        livraisonId, livraison.getStatut());
                continue;
            }

            // Mettre à jour le statut
            livraison.setStatut(StatutLivraison.RAMASSE);
            livraison.setDateRamassage(LocalDateTime.now());

            livraisonRepository.save(livraison);

            // 1️⃣ Notifier le VENDEUR
            notificationService.notifierRamassage(livraison);

            // 2️⃣ Notifier le CLIENT
            String urlTracking = qrCodeService.generateTrackingUrl(livraison.getNumeroTracking());
            String messageClient = String.format(
                    "📦 Votre colis a été récupéré !\n\n" +
                            "Bonjour %s,\n\n" +
                            "Votre colis #%s a été ramassé avec succès.\n" +
                            "Livraison prévue sous 24-48h.\n\n" +
                            "Suivez votre colis: %s\n\n" +
                            "À bientôt ! 😊",
                    livraison.getNomClient(),
                    livraison.getNumeroTracking(),
                    urlTracking
            );
            notificationService.envoyerWhatsApp(
                    livraison.getTelephoneClient(),
                    messageClient
            );

            colisRamasses++;

            log.debug("✅ Colis ramassé: {}", livraison.getNumeroTracking());
        }

        log.info("✅ {} colis marqués comme ramassés sur {} demandés",
                colisRamasses, livraisonIds.size());

        return colisRamasses;
    }

    /**
     * Récupère les statistiques des ramassages
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistiquesRamassages() {
        long totalEnAttente = livraisonRepository.countByStatut(StatutLivraison.EN_ATTENTE_RAMASSAGE);
        long totalRamasses = livraisonRepository.countByStatut(StatutLivraison.RAMASSE);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEnAttente", totalEnAttente);
        stats.put("totalRamasses", totalRamasses);
        stats.put("totalRamassesAujourdhui", getRamassagesAujourdhui().values().stream()
                .mapToInt(List::size)
                .sum());

        return stats;
    }
}