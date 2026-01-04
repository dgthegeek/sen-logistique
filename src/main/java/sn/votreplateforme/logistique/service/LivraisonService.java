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

        // 3. Trouver la zone à partir du quartier
        Zone zone = zoneService.findZoneByQuartier(request.getQuartier(), request.getCommune());

        // 3. Calculer le tarif de livraison
        // Convertir le DTO TypeUrgence en Entity TypeUrgence
        TypeUrgence urgenceEntity = (request.getUrgence() != null)
                ? TypeUrgence.valueOf(request.getUrgence().name())
                : TypeUrgence.NORMAL;
        if (request.getPoids() == null) {request.setPoids(3d); }

        BigDecimal fraisLivraison = tarifCalculator.calculer(zone, urgenceEntity, request.getPoids());

        log.debug("Tarif calculé: {} FCFA (urgence: {}, poids: {} kg)",
                fraisLivraison, urgenceEntity, request.getPoids());

        // 4. Générer le numéro de tracking
        String numeroTracking = trackingNumberGenerator.generate();

        log.debug("Numéro de tracking généré: {}", numeroTracking);

        // 5. Générer l'URL du QR code
        String qrCodeUrl = qrCodeService.generateQRCodeUrl(numeroTracking);

        // 6. Créer l'adresse de destination
        Adresse adresse = new Adresse();
        adresse.setCommune(request.getCommune());
        adresse.setQuartier(request.getQuartier());
        adresse.setAdresseComplete(request.getAdresseComplete());
        adresse.setPointRepere(request.getPointRepere());
        adresse.setZone(zone);

        // 7. Créer la livraison
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

        // Financier
        livraison.setMontantCOD(request.getMontantCOD());
        livraison.setFraisLivraison(fraisLivraison);

        // Statut et options
        livraison.setStatut(StatutLivraison.EN_ATTENTE_RAMASSAGE);
        livraison.setUrgence(urgenceEntity);

        // Convertir CreneauSouhaiteEnum en String
        if (request.getCreneauSouhaite() != null) {
            livraison.setCreneauSouhaite(request.getCreneauSouhaite().name());
        }

        livraison.setNotesPourLivreur(request.getNotesPourLivreur());

        // 8. Sauvegarder
        livraison = livraisonRepository.save(livraison);

        log.info("✅ Livraison créée avec succès: {} (ID: {})", numeroTracking, livraison.getId());

        // 9. Calculer le montant que le vendeur recevra
        BigDecimal montantARecevoir = request.getMontantCOD().subtract(fraisLivraison);

        // 10. Créer la réponse
        return buildLivraisonResponse(livraison, montantARecevoir);
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