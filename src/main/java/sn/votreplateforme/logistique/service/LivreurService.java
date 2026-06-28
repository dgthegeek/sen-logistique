package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.CommandeLivreur;
import sn.votreplateforme.logistique.dto.EchecRequest;
import sn.votreplateforme.logistique.dto.LivrerRequest;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.Livreur;
import sn.votreplateforme.logistique.entity.MotifEchec;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.exception.BusinessException;
import sn.votreplateforme.logistique.exception.ForbiddenException;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.LivreurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de l'interface Livreur.
 *
 * Le livreur ne voit et n'agit que sur SES propres livraisons assignées.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LivreurService {

    /** Livraisons actives par défaut dans "Mes livraisons". */
    private static final List<StatutLivraison> EN_COURS = List.of(
            StatutLivraison.ASSIGNEE,
            StatutLivraison.EN_LIVRAISON
    );

    private final LivraisonRepository livraisonRepository;
    private final LivreurRepository livreurRepository;

    @Transactional(readOnly = true)
    public List<CommandeLivreur> mesLivraisons(sn.votreplateforme.logistique.dto.StatutLivraison statutDto) {
        Livreur livreur = getCurrentLivreur();

        List<StatutLivraison> statuts = EN_COURS;
        if (statutDto != null) {
            statuts = List.of(StatutLivraison.valueOf(statutDto.name()));
        }

        List<CommandeLivreur> livraisons = livraisonRepository
                .findByLivreur_IdAndStatutInOrderByDateAssignationDesc(livreur.getId(), statuts)
                .stream()
                .map(this::mapToCommandeLivreur)
                .collect(Collectors.toList());

        log.info("Livreur {} : {} livraison(s)", livreur.getNomComplet(), livraisons.size());
        return livraisons;
    }

    @Transactional
    public CommandeLivreur commencer(Long id) {
        Livraison l = getMaLivraison(id);
        if (l.getStatut() != StatutLivraison.ASSIGNEE) {
            throw new BusinessException("Cette livraison doit être au statut Assignée pour être démarrée");
        }
        l.marquerEnLivraison();
        log.info("Livraison {} démarrée", l.getNumeroTracking());
        return mapToCommandeLivreur(livraisonRepository.save(l));
    }

    @Transactional
    public CommandeLivreur livrer(Long id, LivrerRequest request) {
        Livraison l = getMaLivraison(id);
        if (l.getStatut() != StatutLivraison.EN_LIVRAISON && l.getStatut() != StatutLivraison.ASSIGNEE) {
            throw new BusinessException("Cette livraison ne peut pas être marquée comme livrée (statut: "
                    + l.getStatut() + ")");
        }
        if (request == null || request.getCashCollecte() == null) {
            throw new BusinessException("Le montant encaissé (cashCollecte) est obligatoire");
        }
        l.marquerLivree(request.getCashCollecte(), request.getCommentaire());
        log.info("Livraison {} livrée - cash collecté: {}", l.getNumeroTracking(), request.getCashCollecte());
        return mapToCommandeLivreur(livraisonRepository.save(l));
    }

    @Transactional
    public CommandeLivreur echec(Long id, EchecRequest request) {
        Livraison l = getMaLivraison(id);
        if (l.estTerminee()) {
            throw new BusinessException("Cette livraison est déjà terminée");
        }
        if (request == null || request.getMotif() == null) {
            throw new BusinessException("Le motif d'échec est obligatoire");
        }
        MotifEchec motif = MotifEchec.valueOf(request.getMotif().name());
        l.marquerEchec(motif, request.getCommentaire());
        log.info("Livraison {} en échec - motif: {}", l.getNumeroTracking(), motif);
        return mapToCommandeLivreur(livraisonRepository.save(l));
    }

    // ==================== HELPERS ====================

    private Livreur getCurrentLivreur() {
        String telephone = SecurityUtils.getCurrentUserTelephone();
        if (telephone == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return livreurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new ForbiddenException("Aucun livreur associé à ce compte"));
    }

    /** Récupère une livraison en s'assurant qu'elle appartient bien au livreur connecté. */
    private Livraison getMaLivraison(Long id) {
        Livreur livreur = getCurrentLivreur();
        Livraison l = livraisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison non trouvée: " + id));
        if (l.getLivreur() == null || !l.getLivreur().getId().equals(livreur.getId())) {
            throw new ForbiddenException("Cette livraison ne vous est pas assignée");
        }
        return l;
    }

    private CommandeLivreur mapToCommandeLivreur(Livraison l) {
        CommandeLivreur c = new CommandeLivreur();
        c.setId(l.getId());
        c.setNumeroTracking(l.getNumeroTracking());
        c.setStatut(sn.votreplateforme.logistique.dto.StatutLivraison.valueOf(l.getStatut().name()));
        c.setNomClient(l.getNomClient());
        c.setTelephoneClient(l.getTelephoneClient());
        if (l.getAdresseDestination() != null) {
            c.setAdresse(l.getAdresseDestination().getAdresseComplete());
            c.setPointRepere(l.getAdresseDestination().getPointRepere());
            if (l.getAdresseDestination().getZone() != null) {
                c.setZone(l.getAdresseDestination().getZone().getNom());
            }
        }
        c.setProduit(l.getDescriptionProduit());
        c.setMontantAEncaisser(l.getMontantCOD());
        return c;
    }
}
