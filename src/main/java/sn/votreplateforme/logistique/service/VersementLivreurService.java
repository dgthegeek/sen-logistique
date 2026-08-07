package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.LivreurFinances;
import sn.votreplateforme.logistique.dto.LivreurSolde;
import sn.votreplateforme.logistique.dto.PageVersement;
import sn.votreplateforme.logistique.dto.VerserLivreurRequest;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.Livreur;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.entity.VersementLivreur;
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.LivreurRepository;
import sn.votreplateforme.logistique.repository.UserRepository;
import sn.votreplateforme.logistique.repository.VersementLivreurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gère l'argent (cash COD) collecté par les livreurs et qu'ils doivent reverser
 * au coordinateur logistique / à l'admin.
 *
 * <p>Le "solde à régler" d'un livreur = somme du cash collecté sur ses livraisons
 * LIVREE non encore reversées. Lorsqu'on marque un livreur comme "versé", on crée
 * un {@link VersementLivreur} (trace horodatée) et toutes ses livraisons non réglées
 * passent à {@code verseLivreur = true} : son solde retombe alors à zéro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersementLivreurService {

    private final LivraisonRepository livraisonRepository;
    private final LivreurRepository livreurRepository;
    private final VersementLivreurRepository versementRepository;
    private final UserRepository userRepository;

    /** Situation financière de chaque livreur (pour le coordinateur/admin). */
    @Transactional(readOnly = true)
    public List<LivreurSolde> soldesLivreurs() {
        return livreurRepository.findAll().stream()
                .map(this::toSolde)
                .sorted(Comparator.comparing(LivreurSolde::getSoldeARegler,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private LivreurSolde toSolde(Livreur livreur) {
        Long id = livreur.getId();
        LivreurSolde s = new LivreurSolde();
        s.setLivreurId(id);
        s.setNom(livreur.getNom());
        s.setPrenom(livreur.getPrenom());
        s.setTelephone(livreur.getTelephone());
        s.setSoldeARegler(livraisonRepository.soldeAReglerLivreur(id));
        s.setNombreLivraisonsNonReglees((int) livraisonRepository
                .countByLivreur_IdAndStatutAndVerseLivreurFalse(id, StatutLivraison.LIVREE));
        s.setTotalCollecte(livraisonRepository.totalCollecteLivreur(id));
        s.setTotalVerse(livraisonRepository.totalVerseLivreur(id));
        return s;
    }

    /**
     * Marque tout le cash non réglé d'un livreur comme versé : crée un versement
     * (historique) et remet le solde du livreur à zéro.
     */
    @Transactional
    public sn.votreplateforme.logistique.dto.VersementLivreur verserLivreur(
            Long livreurId, VerserLivreurRequest request) {

        Livreur livreur = livreurRepository.findById(livreurId)
                .orElseThrow(() -> new NotFoundException("Livreur non trouvé : " + livreurId));

        List<Livraison> aRegler = livraisonRepository
                .findByLivreur_IdAndStatutAndVerseLivreurFalse(livreurId, StatutLivraison.LIVREE);

        if (aRegler.isEmpty()) {
            throw new BadRequestException("Aucun montant à verser pour ce livreur");
        }

        BigDecimal montant = aRegler.stream()
                .map(l -> l.getCashCollecte() != null ? l.getCashCollecte() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        User acteur = userRepository.findByTelephone(SecurityUtils.getCurrentUserTelephone()).orElse(null);

        VersementLivreur versement = VersementLivreur.builder()
                .livreur(livreur)
                .montant(montant)
                .nombreLivraisons(aRegler.size())
                .effectuePar(acteur != null ? acteur.getNomComplet() : null)
                .effectueParRole(acteur != null && acteur.getRole() != null ? acteur.getRole().name() : null)
                .commentaire(request != null ? request.getCommentaire() : null)
                .build();
        versement = versementRepository.save(versement);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Livraison l : aRegler) {
            l.setVerseLivreur(true);
            l.setDateVersementLivreur(now);
            l.setVersement(versement);
        }
        livraisonRepository.saveAll(aRegler);

        log.info("Versement livreur {} : {} FCFA sur {} livraison(s) validé par {}",
                livreurId, montant, aRegler.size(), versement.getEffectuePar());

        return toDto(versement);
    }

    /** Historique paginé de tous les versements. */
    @Transactional(readOnly = true)
    public PageVersement historique(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 50);
        Page<VersementLivreur> p = versementRepository.findAllByOrderByDateVersementDesc(pageable);

        PageVersement result = new PageVersement();
        result.setContent(p.getContent().stream().map(this::toDto).collect(Collectors.toList()));
        result.setPage(p.getNumber());
        result.setSize(p.getSize());
        result.setTotalElements((int) p.getTotalElements());
        result.setTotalPages(p.getTotalPages());
        return result;
    }

    /** Vue financière d'un livreur pour lui-même. */
    @Transactional(readOnly = true)
    public LivreurFinances mesFinances(Long livreurId) {
        LivreurFinances f = new LivreurFinances();
        f.setTotalCollecte(livraisonRepository.totalCollecteLivreur(livreurId));
        f.setTotalVerse(livraisonRepository.totalVerseLivreur(livreurId));
        f.setSoldeARegler(livraisonRepository.soldeAReglerLivreur(livreurId));
        f.setNombreLivraisons((int) livraisonRepository
                .countByLivreur_IdAndStatut(livreurId, StatutLivraison.LIVREE));
        f.setNombreLivraisonsNonReglees((int) livraisonRepository
                .countByLivreur_IdAndStatutAndVerseLivreurFalse(livreurId, StatutLivraison.LIVREE));
        f.setVersements(versementRepository.findByLivreur_IdOrderByDateVersementDesc(livreurId)
                .stream().map(this::toDto).collect(Collectors.toList()));
        return f;
    }

    private sn.votreplateforme.logistique.dto.VersementLivreur toDto(VersementLivreur v) {
        sn.votreplateforme.logistique.dto.VersementLivreur dto =
                new sn.votreplateforme.logistique.dto.VersementLivreur();
        dto.setId(v.getId());
        if (v.getLivreur() != null) {
            dto.setLivreurId(v.getLivreur().getId());
            dto.setLivreurNom(v.getLivreur().getNomComplet());
        }
        dto.setMontant(v.getMontant());
        dto.setNombreLivraisons(v.getNombreLivraisons());
        if (v.getDateVersement() != null) {
            dto.setDateVersement(v.getDateVersement().atOffset(java.time.ZoneOffset.UTC));
        }
        dto.setEffectuePar(v.getEffectuePar());
        dto.setEffectueParRole(v.getEffectueParRole());
        dto.setCommentaire(v.getCommentaire());
        return dto;
    }
}
