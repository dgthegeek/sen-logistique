package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.entity.Zone;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.ZoneRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service Admin Livraisons
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminLivraisonService {

    private final LivraisonRepository livraisonRepository;
    private final ZoneRepository zoneRepository;

    /**
     * Récupère toutes les livraisons avec pagination et filtres
     */
    @Transactional(readOnly = true)
    public PageLivraison getAllLivraisons(
            sn.votreplateforme.logistique.dto.StatutLivraison statutDto,
            LocalDate date,
            Integer page,
            Integer size
    ) {
        log.info("Récupération liste livraisons - Statut: {}, Date: {}", statutDto, date);

        // Valeurs par défaut
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 50;

        // Créer le Pageable
        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by("dateCreation").descending());

        // Convertir le statut DTO en Entity
        StatutLivraison statutEntity = null;
        if (statutDto != null) {
            statutEntity = StatutLivraison.valueOf(statutDto.name());
        }

        // Calculer les dates si date fournie
        LocalDateTime debut = null;
        LocalDateTime fin = null;
        if (date != null) {
            debut = date.atStartOfDay();
            fin = date.atTime(23, 59, 59);
        }

        // Récupérer les livraisons
        Page<Livraison> livraisonsPage = livraisonRepository.rechercherLivraisons(
                null, // vendeur = null (toutes les livraisons)
                statutEntity,
                debut,
                fin,
                pageable
        );

        // Convertir en DTOs
        List<LivraisonResponse> content = livraisonsPage.getContent().stream()
                .map(this::mapToLivraisonResponse)
                .collect(Collectors.toList());

        // Créer la réponse
        PageLivraison response = new PageLivraison();
        response.setContent(content);
        response.setPage(pageNumber);
        response.setSize(pageSize);
        response.setTotalElements((int) livraisonsPage.getTotalElements());
        response.setTotalPages(livraisonsPage.getTotalPages());

        log.info("✅ {} livraisons récupérées", content.size());

        return response;
    }

    /**
     * Récupère les colis à livrer (statut RAMASSE), groupés par zone
     */
    @Transactional(readOnly = true)
    public AdminLivraisonsALivrerGet200Response getColisALivrer(String zoneNom) {
        log.info("Récupération colis à livrer - Zone: {}", zoneNom);

        // Récupérer les livraisons avec statut RAMASSE
        List<Livraison> livraisonsRamassees = livraisonRepository
                .findByStatut(StatutLivraison.RAMASSE);

        // Filtrer par zone si spécifié
        if (zoneNom != null && !zoneNom.isEmpty()) {
            livraisonsRamassees = livraisonsRamassees.stream()
                    .filter(l -> l.getAdresseDestination().getZone().getNom()
                            .equalsIgnoreCase(zoneNom))
                    .collect(Collectors.toList());
        }

        // Grouper par zone
        Map<String, List<Livraison>> parZone = livraisonsRamassees.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getAdresseDestination().getZone().getNom()
                ));

        // Construire les LivraisonZone
        List<LivraisonZone> zones = new ArrayList<>();
        int totalColis = 0;

        for (Map.Entry<String, List<Livraison>> entry : parZone.entrySet()) {
            String nomZone = entry.getKey();
            List<Livraison> livraisons = entry.getValue();

            LivraisonZone livraisonZone = new LivraisonZone();
            livraisonZone.setZone(nomZone);
            livraisonZone.setNombreColis(livraisons.size());

            // Créer la liste des colis
            List<LivraisonZoneColisInner> colis = livraisons.stream()
                    .map(this::mapToColisInner)
                    .collect(Collectors.toList());

            livraisonZone.setColis(colis);
            zones.add(livraisonZone);
            totalColis += livraisons.size();
        }

        // Construire la réponse
        AdminLivraisonsALivrerGet200Response response =
                new AdminLivraisonsALivrerGet200Response();
        response.setZones(zones);
        response.setTotalColis(totalColis);

        log.info("✅ {} colis à livrer dans {} zones", totalColis, zones.size());

        return response;
    }

    /**
     * Récupère les détails complets d'une livraison (admin)
     */
    @Transactional(readOnly = true)
    public LivraisonDetailResponse getDetailLivraison(Long id) {
        log.info("Récupération détails livraison {}", id);

        Livraison livraison = livraisonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Livraison non trouvée: " + id));

        // Construire la réponse détaillée
        LivraisonDetailResponse detail = new LivraisonDetailResponse();
        detail.setId(livraison.getId());
        detail.setNumeroTracking(livraison.getNumeroTracking());
        detail.setQrCodeUrl(livraison.getQrCodeUrl());
        detail.setStatut(sn.votreplateforme.logistique.dto.StatutLivraison.valueOf(
                livraison.getStatut().name()));

        // Dates
        if (livraison.getDateCreation() != null) {
            detail.setDateCreation(livraison.getDateCreation().atOffset(ZoneOffset.UTC));
        }
        if (livraison.getDateRamassage() != null) {
            detail.setDateRamassage(livraison.getDateRamassage().atOffset(ZoneOffset.UTC));
        }
        if (livraison.getDateLivraison() != null) {
            detail.setDateLivraison(livraison.getDateLivraison().atOffset(ZoneOffset.UTC));
        }

        // Informations vendeur
        LivraisonDetailResponseVendeur vendeurInfo = new LivraisonDetailResponseVendeur();
        vendeurInfo.setId(livraison.getVendeur().getId());
        vendeurInfo.setNom(livraison.getVendeur().getNom());
        vendeurInfo.setPrenom(livraison.getVendeur().getPrenom());
        vendeurInfo.setTelephone(livraison.getVendeur().getTelephone());
        vendeurInfo.setNomBoutique(livraison.getVendeur().getNomBoutique());
        detail.setVendeur(vendeurInfo);

        // Informations client
        LivraisonDetailResponseClient clientInfo = new LivraisonDetailResponseClient();
        clientInfo.setNom(livraison.getNomClient());
        clientInfo.setTelephone(livraison.getTelephoneClient());
        clientInfo.setAdresse(livraison.getAdresseDestination().getAdresseComplete());
        clientInfo.setPointRepere(livraison.getAdresseDestination().getPointRepere());
        detail.setClient(clientInfo);

        // Informations produit
        LivraisonDetailResponseProduit produitInfo = new LivraisonDetailResponseProduit();
        produitInfo.setDescription(livraison.getDescriptionProduit());
        produitInfo.setFragile(livraison.getFragile());
        produitInfo.setPoids(livraison.getPoids().doubleValue());
        detail.setProduit(produitInfo);

        // Informations financières
        LivraisonDetailResponseFinancier financierInfo = new LivraisonDetailResponseFinancier();
        financierInfo.setMontantCOD(livraison.getMontantCOD());
        financierInfo.setFraisLivraison(livraison.getFraisLivraison());
        financierInfo.setCashCollecte(livraison.getCashCollecte());
        detail.setFinancier(financierInfo);

        // Zone et urgence
        detail.setZone(livraison.getAdresseDestination().getZone().getNom());
        detail.setUrgence(sn.votreplateforme.logistique.dto.TypeUrgence.valueOf(
                livraison.getUrgence().name()));
        detail.setCommentaireLivraison(livraison.getCommentaireLivraison());

        log.info("✅ Détails livraison {} récupérés", livraison.getNumeroTracking());

        return detail;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Mappe une Livraison en LivraisonResponse
     */
    private LivraisonResponse mapToLivraisonResponse(Livraison livraison) {
        LivraisonResponse response = new LivraisonResponse();
        response.setId(livraison.getId());
        response.setNumeroTracking(livraison.getNumeroTracking());
        response.setQrCodeUrl(livraison.getQrCodeUrl());
        response.setStatut(sn.votreplateforme.logistique.dto.StatutLivraison.valueOf(
                livraison.getStatut().name()));

        if (livraison.getDateCreation() != null) {
            response.setDateCreation(livraison.getDateCreation().atOffset(ZoneOffset.UTC));
        }

        response.setFraisLivraison(livraison.getFraisLivraison());
        response.setMontantCOD(livraison.getMontantCOD());

        BigDecimal montantARecevoir = livraison.getMontantCOD()
                .subtract(livraison.getFraisLivraison());
        response.setMontantARecevoir(montantARecevoir);

        // Message selon statut
        String message = switch (livraison.getStatut()) {
            case EN_ATTENTE_RAMASSAGE -> "En attente de ramassage";
            case RAMASSE -> "Colis ramassé";
            case EN_ROUTE -> "En cours de livraison";
            case LIVREE -> "Livré";
            case ANNULEE -> "Annulé";
            default -> livraison.getStatut().name();
        };
        response.setMessage(message);

        return response;
    }

    /**
     * Mappe une Livraison en LivraisonZoneColisInner
     */
    private LivraisonZoneColisInner mapToColisInner(Livraison livraison) {
        LivraisonZoneColisInner colis = new LivraisonZoneColisInner();
        colis.setId(livraison.getId());
        colis.setNumeroTracking(livraison.getNumeroTracking());
        colis.setNomClient(livraison.getNomClient());
        colis.setTelephoneClient(livraison.getTelephoneClient());
        colis.setAdresse(livraison.getAdresseDestination().getAdresseComplete());
        colis.setMontantCOD(livraison.getMontantCOD());

        // Nom complet du vendeur
        String vendeur = livraison.getVendeur().getPrenom() + " " +
                livraison.getVendeur().getNom();
        colis.setVendeur(vendeur);

        return colis;
    }
}