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
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendeurAdminService {

    private final VendeurRepository vendeurRepository;
    private final LivraisonRepository livraisonRepository;

    @Transactional(readOnly = true)
    public AdminVendeursEnAttenteGet200Response getVendeursEnAttente() {
        List<Vendeur> vendeurs = vendeurRepository.findByStatut(
                sn.votreplateforme.logistique.dto.StatutVendeur.EN_ATTENTE_VALIDATION
        );

        AdminVendeursEnAttenteGet200Response response = new AdminVendeursEnAttenteGet200Response();
        response.setVendeurs(vendeurs.stream().map(this::toVendeurDTO).toList());
        response.setTotal(vendeurs.size());

        return response;
    }

    @Transactional(readOnly = true)
    public PageVendeur getAllVendeurs(
            StatutVendeur statutDTO,
            String quartier,
            String commune,
            String search,
            String sortField,
            String order,
            Integer page,
            Integer size
    ) {
        // Convertir enum en String
        String statutString = statutDTO != null ? statutDTO.getValue() : null;

        Pageable pageable = PageRequest.of(page, size);

        // 1. Récupérer les IDs seulement (évite le password)
        List<Long> ids = vendeurRepository.searchVendeurIds(
                statutString,
                quartier,
                commune,
                search,
                pageable
        );

        // 2. Charger les vendeurs par IDs (JPA gère bien ça)
        List<Vendeur> vendeurs = vendeurRepository.findAllById(ids);

        // 3. Count total
        long total = vendeurRepository.countVendeurs(
                statutString,
                quartier,
                commune,
                search
        );

        // Mapper vers DTO
        PageVendeur pageVendeur = new PageVendeur();
        pageVendeur.setContent(vendeurs.stream()
                .map(this::toVendeurDTO)
                .toList());
        pageVendeur.setPage(page);
        pageVendeur.setSize(size);
        pageVendeur.setTotalElements((int) total);
        pageVendeur.setTotalPages((int) Math.ceil((double) total / size));

        return pageVendeur;
    }

    @Transactional(readOnly = true)
    public VendeurDetailDTO getVendeurDetail(Long vendeurId) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé"));

        VendeurDetailDTO dto = toVendeurDetailDTO(vendeur);

        // Ajouter les statistiques
        dto.setStatistiques(calculerStatistiques(vendeur));

        // Ajouter l'admin qui a validé si existe
        if (vendeur.getValidePar() != null) {
            VendeurDetailDTOAllOfValidePar validePar = new VendeurDetailDTOAllOfValidePar();
            validePar.setId(vendeur.getValidePar().getId());
            validePar.setNom(vendeur.getValidePar().getNom());
            validePar.setPrenom(vendeur.getValidePar().getPrenom());
            dto.setValidePar(validePar);
        }

        return dto;
    }

    @Transactional
    public AdminVendeursIdValiderPost200Response validerVendeur(Long vendeurId, java.math.BigDecimal commission) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé"));

        if (vendeur.getStatut() != sn.votreplateforme.logistique.dto.StatutVendeur.EN_ATTENTE_VALIDATION) {
            throw new BadRequestException("Le vendeur n'est pas en attente de validation");
        }

        // Mettre à jour le statut
        vendeur.setStatut(sn.votreplateforme.logistique.dto.StatutVendeur.ACTIF);
        vendeur.setValideLe(LocalDateTime.now());
        vendeur.setRaisonSuspension(null);

        // Commission fixe (prix de livraison) réglée par l'admin à la validation
        if (commission != null) {
            vendeur.setCommissionFixe(commission);
        }

        vendeurRepository.save(vendeur);

        AdminVendeursIdValiderPost200Response response = new AdminVendeursIdValiderPost200Response();
        response.setMessage("Vendeur validé avec succès");
        response.setVendeur(toVendeurDTO(vendeur));

        return response;
    }

    /** Définit / modifie la commission fixe (prix de livraison) d'un vendeur. */
    @Transactional
    public VendeurDTO setCommission(Long vendeurId, java.math.BigDecimal commission) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé"));
        vendeur.setCommissionFixe(commission);
        vendeurRepository.save(vendeur);
        return toVendeurDTO(vendeur);
    }

    @Transactional
    public AdminVendeursIdSuspendrePost200Response suspendreVendeur(Long vendeurId, String raison) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé"));

        if (vendeur.getStatut() != sn.votreplateforme.logistique.dto.StatutVendeur.ACTIF) {
            throw new BadRequestException("Seul un vendeur ACTIF peut être suspendu");
        }

        vendeur.setStatut(sn.votreplateforme.logistique.dto.StatutVendeur.SUSPENDU);
        vendeur.setRaisonSuspension(raison);

        vendeurRepository.save(vendeur);

        AdminVendeursIdSuspendrePost200Response response = new AdminVendeursIdSuspendrePost200Response();
        response.setMessage("Vendeur suspendu");
        response.setVendeur(toVendeurDTO(vendeur));

        return response;
    }

    @Transactional
    public AdminVendeursIdBloquerPost200Response bloquerVendeur(Long vendeurId, String raison) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé"));

        vendeur.setStatut(sn.votreplateforme.logistique.dto.StatutVendeur.BLOQUE);
        vendeur.setRaisonSuspension(raison);

        vendeurRepository.save(vendeur);

        AdminVendeursIdBloquerPost200Response response = new AdminVendeursIdBloquerPost200Response();
        response.setMessage("Vendeur bloqué définitivement");
        response.setVendeur(toVendeurDTO(vendeur));

        return response;
    }

    @Transactional
    public AdminVendeursIdReactiverPost200Response reactiverVendeur(Long vendeurId) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new NotFoundException("Vendeur non trouvé"));

        if (vendeur.getStatut() != sn.votreplateforme.logistique.dto.StatutVendeur.SUSPENDU) {
            throw new BadRequestException("Seul un vendeur SUSPENDU peut être réactivé");
        }

        vendeur.setStatut(sn.votreplateforme.logistique.dto.StatutVendeur.ACTIF);
        vendeur.setRaisonSuspension(null);

        vendeurRepository.save(vendeur);

        AdminVendeursIdReactiverPost200Response response = new AdminVendeursIdReactiverPost200Response();
        response.setMessage("Vendeur réactivé avec succès");
        response.setVendeur(toVendeurDTO(vendeur));

        return response;
    }

    // ===== MÉTHODES PRIVÉES =====

    private VendeurDetailDTOAllOfStatistiques calculerStatistiques(Vendeur vendeur) {
        List<Livraison> livraisons = livraisonRepository.findByVendeurId(vendeur.getId());

        int nombreLivraisons = livraisons.size();
        int nombreReussies = (int) livraisons.stream()
                .filter(l -> l.getStatut() == sn.votreplateforme.logistique.entity.StatutLivraison.LIVREE)
                .count();
        int nombreEnCours = (int) livraisons.stream()
                .filter(l -> l.getStatut() != sn.votreplateforme.logistique.entity.StatutLivraison.LIVREE
                        && l.getStatut() != sn.votreplateforme.logistique.entity.StatutLivraison.ANNULEE
                        && l.getStatut() != sn.votreplateforme.logistique.entity.StatutLivraison.ECHEC_ABSENT
                        && l.getStatut() != sn.votreplateforme.logistique.entity.StatutLivraison.ECHEC_REFUSE)
                .count();

        double tauxReussite = nombreLivraisons > 0
                ? (nombreReussies * 100.0) / nombreLivraisons
                : 0.0;

        BigDecimal caTotal = livraisons.stream()
                .filter(l -> l.getStatut() == sn.votreplateforme.logistique.entity.StatutLivraison.LIVREE)
                .map(Livraison::getMontantCOD)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime derniereActivite = livraisons.stream()
                .map(Livraison::getDateCreation)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        VendeurDetailDTOAllOfStatistiques stats = new VendeurDetailDTOAllOfStatistiques();
        stats.setNombreLivraisons(nombreLivraisons);
        stats.setNombreLivraisonsReussies(nombreReussies);
        stats.setNombreEnCours(nombreEnCours);
        stats.setTauxReussite(BigDecimal.valueOf(tauxReussite).setScale(2, RoundingMode.HALF_UP).doubleValue());
        stats.setChiffreAffairesTotal(caTotal);

        // Convertir LocalDateTime en OffsetDateTime si non null
        if (derniereActivite != null) {
            stats.setDerniereActivite(derniereActivite.atOffset(ZoneOffset.UTC));
        }

        return stats;
    }

    private VendeurDTO toVendeurDTO(Vendeur vendeur) {
        VendeurDTO dto = new VendeurDTO();
        dto.setId(vendeur.getId());
        dto.setNom(vendeur.getNom());
        dto.setPrenom(vendeur.getPrenom());
        dto.setTelephone(vendeur.getTelephone());
        dto.setEmail(vendeur.getEmail());
        dto.setNomBoutique(vendeur.getNomBoutique());
        dto.setCategorieActivite(vendeur.getCategorieActivite());
        dto.setInstagram(vendeur.getInstagram());
        dto.setFacebook(vendeur.getFacebook());
        dto.setCommune(vendeur.getCommune());
        dto.setQuartier(vendeur.getQuartier());
        dto.setAdresseComplete(vendeur.getAdresseComplete());
        dto.setStatut(StatutVendeur.fromValue(vendeur.getStatut().name()));

        // Convertir LocalDateTime en OffsetDateTime
        if (vendeur.getDateInscription() != null) {
            dto.setDateInscription(vendeur.getDateInscription().atOffset(ZoneOffset.UTC));
        }
        if (vendeur.getValideLe() != null) {
            dto.setValideLe(vendeur.getValideLe().atOffset(ZoneOffset.UTC));
        }

        dto.setSoldeEnAttente(vendeur.getSoldeEnAttente());
        dto.setCommissionFixe(vendeur.getCommissionFixe());
        dto.setRaisonSuspension(vendeur.getRaisonSuspension());
        return dto;
    }

    private VendeurDetailDTO toVendeurDetailDTO(Vendeur vendeur) {
        VendeurDetailDTO dto = new VendeurDetailDTO();
        dto.setId(vendeur.getId());
        dto.setNom(vendeur.getNom());
        dto.setPrenom(vendeur.getPrenom());
        dto.setTelephone(vendeur.getTelephone());
        dto.setEmail(vendeur.getEmail());
        dto.setNomBoutique(vendeur.getNomBoutique());
        dto.setCategorieActivite(vendeur.getCategorieActivite());
        dto.setInstagram(vendeur.getInstagram());
        dto.setFacebook(vendeur.getFacebook());
        dto.setCommune(vendeur.getCommune());
        dto.setQuartier(vendeur.getQuartier());
        dto.setAdresseComplete(vendeur.getAdresseComplete());
        dto.setStatut(StatutVendeur.fromValue(vendeur.getStatut().name()));

        // Convertir LocalDateTime en OffsetDateTime
        if (vendeur.getDateInscription() != null) {
            dto.setDateInscription(vendeur.getDateInscription().atOffset(ZoneOffset.UTC));
        }
        if (vendeur.getValideLe() != null) {
            dto.setValideLe(vendeur.getValideLe().atOffset(ZoneOffset.UTC));
        }

        dto.setSoldeEnAttente(vendeur.getSoldeEnAttente());
        dto.setCommissionFixe(vendeur.getCommissionFixe());
        dto.setRaisonSuspension(vendeur.getRaisonSuspension());
        return dto;
    }
}