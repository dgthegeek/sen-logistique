package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.CreateLivraisonRequest;
import sn.votreplateforme.logistique.dto.LivraisonResponse;
import sn.votreplateforme.logistique.dto.PageLivraison;
import sn.votreplateforme.logistique.entity.*;
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.ProduitRepository;
import sn.votreplateforme.logistique.repository.UserRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;
import sn.votreplateforme.logistique.util.TarifCalculator;
import sn.votreplateforme.logistique.util.TrackingNumberGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.entity.Admin;
import sn.votreplateforme.logistique.exception.ForbiddenException;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des livraisons
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LivraisonService {

    private final LivraisonRepository livraisonRepository;
    private final VendeurRepository vendeurRepository;
    private final ZoneService zoneService;
    private final QRCodeService qrCodeService;
    private final TarifCalculator tarifCalculator;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProduitRepository produitRepository;
    private final TelegramService telegramService;

    /**
     * Crée une nouvelle livraison
     */
    @Transactional
    public LivraisonResponse creerLivraison(CreateLivraisonRequest request) {
        log.info("=== Création d'une nouvelle livraison ===");

        // 1. Récupérer l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String telephoneConnecte = auth.getName();

        User userConnecte = userRepository.findByTelephone(telephoneConnecte)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        // 2. Déterminer le vendeur
        Vendeur vendeur;

        if (userConnecte instanceof Admin) {
            // Admin → doit fournir telephoneVendeur
            if (request.getTelephoneVendeur() == null || request.getTelephoneVendeur().isEmpty()) {
                throw new BadRequestException("Le champ telephoneVendeur est requis pour un admin");
            }

            vendeur = vendeurRepository.findByTelephone(request.getTelephoneVendeur())
                    .orElseThrow(() -> new NotFoundException("Vendeur non trouvé : " + request.getTelephoneVendeur()));

            log.info("👤 Admin {} crée une livraison pour le vendeur {}",
                    telephoneConnecte, request.getTelephoneVendeur());

        } else if (userConnecte instanceof Vendeur) {
            // Vendeur → utilise son propre compte (ignore telephoneVendeur)
            vendeur = (Vendeur) userConnecte;

            log.info("👤 Vendeur {} crée sa propre livraison", telephoneConnecte);

        } else {
            throw new ForbiddenException("Seuls les vendeurs et admins peuvent créer des livraisons");
        }

        log.debug("Vendeur: {} {} (ID: {})", vendeur.getPrenom(), vendeur.getNom(), vendeur.getId());

        // 3. Trouver la zone : d'abord via le quartier (précis, sans lever d'exception),
        //    sinon repli sur la zone fournie (dérivée de la commune) pour autoriser une
        //    adresse libre / hors liste. On évite tout throw ici : une exception, même
        //    rattrapée, marquerait la transaction en rollback-only.
        Zone zone = null;
        if (request.getQuartier() != null && !request.getQuartier().isBlank()) {
            zone = zoneService.findZoneByQuartierOptional(request.getQuartier(), request.getCommune())
                    .orElse(null);
        }
        if (zone == null) {
            if (request.getZoneId() == null) {
                throw new IllegalArgumentException(
                        "Impossible de déterminer la zone de livraison (quartier hors liste et zone non fournie)");
            }
            zone = zoneService.findById(request.getZoneId());
        }

        // 4. Calculer le tarif de livraison
        TypeUrgence urgenceEntity = (request.getUrgence() != null)
                ? TypeUrgence.valueOf(request.getUrgence().name())
                : TypeUrgence.NORMAL;
        if (request.getPoids() == null) {
            request.setPoids(3d);
        }

        // Prix de livraison = commission fixe négociée avec le vendeur (réglée par l'admin).
        // Repli sur la tarification par zone tant que la commission n'a pas été fixée.
        BigDecimal fraisLivraison;
        if (vendeur.getCommissionFixe() != null) {
            fraisLivraison = vendeur.getCommissionFixe();
            log.debug("Commission fixe vendeur appliquée: {} FCFA", fraisLivraison);
        } else {
            fraisLivraison = tarifCalculator.calculer(zone, urgenceEntity, request.getPoids());
            log.debug("Tarif zone calculé: {} FCFA (urgence: {}, poids: {} kg)",
                    fraisLivraison, urgenceEntity, request.getPoids());
        }

        // 5. Générer le numéro de tracking
        String numeroTracking = trackingNumberGenerator.generate();

        log.debug("Numéro de tracking généré: {}", numeroTracking);

        // 6. Générer l'URL du QR code
        String qrCodeUrl = qrCodeService.generateQRCodeUrl(numeroTracking);

        // 7. Créer l'adresse de destination
        Adresse adresse = new Adresse();
        adresse.setCommune(request.getCommune());
        adresse.setQuartier(request.getQuartier());
        adresse.setAdresseComplete(request.getAdresseComplete());
        adresse.setPointRepere(request.getPointRepere());
        adresse.setZone(zone);

        // 8. Créer la livraison
        Livraison livraison = new Livraison();
        livraison.setNumeroTracking(numeroTracking);
        livraison.setQrCodeUrl(qrCodeUrl);
        livraison.setVendeur(vendeur);

        // Client
        livraison.setNomClient(request.getNomClient());
        livraison.setTelephoneClient(request.getTelephoneClient());
        livraison.setAdresseDestination(adresse);

        // Colis
        livraison.setDescriptionProduit(request.getDescriptionProduit());
        livraison.setFragile(request.getFragile() != null ? request.getFragile() : false);
        livraison.setPoids(BigDecimal.valueOf(request.getPoids()));

        // ===== Produits liés (multi-produits) pour le décrément auto + calcul du COD =====
        BigDecimal montantProduits = null;
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Multi-produits : on calcule le total à partir des prix catalogue
            montantProduits = BigDecimal.ZERO;
            for (var item : request.getItems()) {
                Produit produit = produitRepository.findById(item.getProduitId())
                        .orElseThrow(() -> new NotFoundException("Produit non trouvé: " + item.getProduitId()));
                int qte = (item.getQuantite() != null && item.getQuantite() > 0) ? item.getQuantite() : 1;

                // Blocage si stock insuffisant
                if (produit.getQuantiteStock() == null || produit.getQuantiteStock() < qte) {
                    throw new BadRequestException("Stock insuffisant pour « " + produit.getNom()
                            + " » : demandé " + qte + ", disponible "
                            + (produit.getQuantiteStock() != null ? produit.getQuantiteStock() : 0));
                }

                BigDecimal prix = produit.getPrixUnitaire() != null ? produit.getPrixUnitaire() : BigDecimal.ZERO;

                LigneCommande ligne = LigneCommande.builder()
                        .produit(produit)
                        .quantite(qte)
                        .prixUnitaire(prix)
                        .build();
                livraison.ajouterLigne(ligne);
                montantProduits = montantProduits.add(prix.multiply(BigDecimal.valueOf(qte)));
            }
        } else if (request.getProduitId() != null) {
            // Compat : produit unique
            Produit produit = produitRepository.findById(request.getProduitId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé: " + request.getProduitId()));
            livraison.setProduit(produit);
            livraison.setQuantite(request.getQuantite() != null && request.getQuantite() > 0
                    ? request.getQuantite() : 1);
        }

        // Financier : le montant est calculé automatiquement à partir des produits, mais le
        // vendeur peut l'ajuster à la saisie (prix flexible). On respecte donc le COD fourni
        // par le client s'il est présent ; sinon on retombe sur le total produits + frais.
        BigDecimal montantCOD;
        if (request.getMontantCOD() != null && request.getMontantCOD().signum() > 0) {
            montantCOD = request.getMontantCOD();
        } else if (montantProduits != null) {
            montantCOD = montantProduits.add(fraisLivraison);
        } else {
            montantCOD = BigDecimal.ZERO;
        }
        livraison.setMontantCOD(montantCOD);
        livraison.setFraisLivraison(fraisLivraison);

        // Statut et options - nouveau cycle Closing : la commande entre en file closeur
        livraison.setStatut(StatutLivraison.NOUVELLE);
        livraison.setUrgence(urgenceEntity);

        // Convertir CreneauSouhaiteEnum en String
        if (request.getCreneauSouhaite() != null) {
            livraison.setCreneauSouhaite(request.getCreneauSouhaite().name());
        }

        livraison.setNotesPourLivreur(request.getNotesPourLivreur());

        // 9. Sauvegarder
        livraison = livraisonRepository.save(livraison);

        log.info("✅ Livraison créée avec succès: {} (ID: {})", numeroTracking, livraison.getId());

        // 10. ENVOYER LES 3 NOTIFICATIONS
        envoyerNotificationsCreation(livraison);

        // 11. Calculer le montant que le vendeur recevra : COD encaissé - commission (frais)
        BigDecimal montantARecevoir = montantCOD.subtract(fraisLivraison);

        // 12. Créer la réponse
        return buildLivraisonResponse(livraison, montantARecevoir);
    }

    /**
     * Envoie les 3 notifications après création d'une livraison
     * 1. Notification au VENDEUR
     * 2. Notification au CLIENT
     * 3. Notification à L'ADMIN
     */
    private void envoyerNotificationsCreation(Livraison livraison) {
        try {
            // 1️⃣ NOTIFICATION AU VENDEUR
            String messageVendeur = String.format(
                    "Nouvelle livraison créée !\n\n" +
                            "📦 Colis : #%s\n" +
                            "📍 Destination : %s, %s\n" +
                            "👤 Client : %s (%s)\n" +
                            "💰 Montant COD : %s FCFA\n" +
                            "📊 Frais livraison : %s FCFA\n" +
                            "💵 Vous recevrez : %s FCFA\n\n" +
                            "Préparez le colis pour le ramassage.\n" +
                            "Merci ! 💚",
                    livraison.getNumeroTracking(),
                    livraison.getAdresseDestination().getQuartier().toUpperCase(),
                    livraison.getAdresseDestination().getCommune().toUpperCase(),
                    livraison.getNomClient().toUpperCase(),
                    livraison.getTelephoneClient(),
                    formatMontant(livraison.getMontantCOD()),
                    formatMontant(livraison.getFraisLivraison()),
                    formatMontant(livraison.getMontantCOD().subtract(livraison.getFraisLivraison()))
            );

            notificationService.envoyerWhatsApp(
                    livraison.getVendeur().getTelephone(),
                    messageVendeur
            );
            log.info("📱 Notification vendeur envoyée : {}", livraison.getVendeur().getTelephone());

            // 2️⃣ NOTIFICATION AU CLIENT
            String messageClient = String.format(
                    "📦 Votre commande de %s a été prise en charge par Dioks !\n\n" +
                            "Bonjour %s,\n\n" +
                            "💰 Montant à payer : %s FCFA\n" +
                            "🔗 Suivez votre colis :\n" +
                            "https://app.dioks.com/tracking/%s\n\n" +
                            "Vous serez notifié quand le livreur sera en route.\n" +
                            "Merci de faire confiance à Dioks ! 😊",
                    livraison.getVendeur().getNomBoutique().toUpperCase(),
                    livraison.getNomClient().toUpperCase(),
                    formatMontant(livraison.getMontantCOD()),
                    livraison.getNumeroTracking()
            );

            notificationService.envoyerWhatsApp(
                    livraison.getTelephoneClient(),
                    messageClient
            );
            log.info("📱 Notification client envoyée : {}", livraison.getTelephoneClient());

            // 3️⃣ NOTIFICATION À L'ADMIN
            String messageAdmin = String.format(
                    "Nouveau ramassage à effectuer\n\n" +
                            "Colis : #%s\n" +
                            "🏪 Vendeur : %s %s\n" +
                            "📱 Tél vendeur : %s\n\n" +
                            "📍 Zone : %s\n" +
                            "🎯 Destination : %s, %s\n" +
                            "👤 Client : %s (%s)\n\n" +
                            "💰 COD : %s FCFA\n" +
                            "📊 Frais : %s FCFA\n" +
                            "⚡ Urgence : %s\n" +
                            "%s\n" +
                            "À ramasser dès que possible !",
                    livraison.getNumeroTracking(),
                    livraison.getVendeur().getPrenom().toUpperCase(),
                    livraison.getVendeur().getNom().toUpperCase(),
                    livraison.getVendeur().getTelephone(),
                    livraison.getAdresseDestination().getZone().getNom().toUpperCase(),
                    livraison.getAdresseDestination().getQuartier().toUpperCase(),
                    livraison.getAdresseDestination().getCommune().toUpperCase(),
                    livraison.getNomClient().toUpperCase(),
                    livraison.getTelephoneClient(),
                    formatMontant(livraison.getMontantCOD()),
                    formatMontant(livraison.getFraisLivraison()),
                    livraison.getUrgence(),
                    livraison.getFragile() ? "⚠️ FRAGILE" : ""
            );

            notificationService.envoyerNotificationAdmin(
                    "Nouveau ramassage",
                    messageAdmin
            );
            log.info("📱 Notification admin envoyée");

            // 4️⃣ NOTIFICATION TELEGRAM AUX CLOSEURS : nouvelle commande à prendre en charge
            telegramService.notifyCloseurs(String.format(
                    "🆕 <b>Nouvelle commande à prendre en charge</b>\n"
                            + "N° %s\n\n"
                            + "👤 <b>Client :</b> %s\n"
                            + "📞 %s\n"
                            + "📍 %s, %s\n\n"
                            + "À appeler pour confirmation.",
                    livraison.getNumeroTracking(),
                    livraison.getNomClient(),
                    livraison.getTelephoneClient(),
                    livraison.getAdresseDestination().getQuartier(),
                    livraison.getAdresseDestination().getCommune()
            ));

        } catch (Exception e) {
            // Ne pas bloquer la création de livraison si notifications échouent
            log.error("❌ Erreur lors de l'envoi des notifications : {}", e.getMessage());
        }
    }

    /**
     * Formate un montant BigDecimal en FCFA avec séparateurs
     */
    private String formatMontant(BigDecimal montant) {
        if (montant == null) {
            return "0";
        }
        return String.format("%,d", montant.longValue()).replace(',', ' ');
    }

    /**
     * Récupère les livraisons du vendeur connecté
     */
    @Transactional(readOnly = true)
    public PageLivraison getMesLivraisons(sn.votreplateforme.logistique.dto.StatutLivraison statutDto, int page, int size) {
        log.debug("Récupération des livraisons du vendeur - Statut: {}, Page: {}, Size: {}",
                statutDto, page, size);

        // Récupérer le vendeur connecté
        String telephone = SecurityUtils.getCurrentUserTelephone();
        Vendeur vendeur = vendeurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new IllegalStateException("Vendeur non trouvé"));

        // Créer le Pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());

        // Convertir le DTO StatutLivraison en Entity StatutLivraison (si fourni)
        StatutLivraison statutEntity = null;
        if (statutDto != null) {
            statutEntity = StatutLivraison.valueOf(statutDto.name());
        }

        // Récupérer les livraisons
        Page<Livraison> livraisonsPage;
        if (statutEntity != null) {
            livraisonsPage = livraisonRepository.findByVendeurAndStatut(vendeur, statutEntity, pageable);
        } else {
            livraisonsPage = livraisonRepository.findByVendeur(vendeur, pageable);
        }

        // Convertir en DTO
        List<LivraisonResponse> content = livraisonsPage.getContent().stream()
                .map(l -> buildLivraisonResponse(l, l.getMontantCOD().subtract(l.getFraisLivraison())))
                .collect(Collectors.toList());

        // Créer PageLivraison
        PageLivraison pageLivraison = new PageLivraison();
        pageLivraison.setContent(content);
        pageLivraison.setPage(page);
        pageLivraison.setSize(size);
        pageLivraison.setTotalElements((int) livraisonsPage.getTotalElements());
        pageLivraison.setTotalPages(livraisonsPage.getTotalPages());

        log.debug("Livraisons récupérées: {} sur {}", content.size(), livraisonsPage.getTotalElements());

        return pageLivraison;
    }

    /**
     * Construit un LivraisonResponse à partir d'une entité Livraison
     */
    private LivraisonResponse buildLivraisonResponse(Livraison livraison, BigDecimal montantARecevoir) {
        LivraisonResponse response = new LivraisonResponse();
        response.setId(livraison.getId());
        response.setNumeroTracking(livraison.getNumeroTracking());
        response.setQrCodeUrl(livraison.getQrCodeUrl());

        // Convertir l'Entity StatutLivraison en DTO StatutLivraison
        response.setStatut(sn.votreplateforme.logistique.dto.StatutLivraison.valueOf(
                livraison.getStatut().name()
        ));

        // Convertir LocalDateTime en OffsetDateTime
        if (livraison.getDateCreation() != null) {
            response.setDateCreation(livraison.getDateCreation().atOffset(ZoneOffset.UTC));
        }

        response.setFraisLivraison(livraison.getFraisLivraison());
        response.setMontantCOD(livraison.getMontantCOD());
        response.setMontantARecevoir(montantARecevoir);

        // Message personnalisé selon le statut
        String message = switch (livraison.getStatut()) {
            case EN_ATTENTE_RAMASSAGE ->
                    "Livraison créée avec succès ! Écrivez #" + livraison.getNumeroTracking() + " sur votre colis.";
            case RAMASSE ->
                    "Colis ramassé et en cours de traitement.";
            case EN_ROUTE ->
                    "Colis en cours de livraison.";
            case LIVREE ->
                    "Colis livré avec succès !";
            default ->
                    "Statut: " + livraison.getStatut();
        };

        response.setMessage(message);

        return response;
    }
}