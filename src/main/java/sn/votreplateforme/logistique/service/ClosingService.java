package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.CommandeCloseur;
import sn.votreplateforme.logistique.dto.CommentaireRequest;
import sn.votreplateforme.logistique.entity.Closeur;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.exception.BusinessException;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.repository.CloseurRepository;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service du module Closing.
 *
 * Le closeur traite les commandes Nouvelle / A appeler / Confirmée :
 * il appelle le client, confirme la commande puis la rend "prête à livrer"
 * pour le dispatch.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClosingService {

    /** Statuts visibles dans la file du closeur. */
    private static final List<StatutLivraison> FILE_CLOSEUR = List.of(
            StatutLivraison.NOUVELLE,
            StatutLivraison.A_APPELER,
            StatutLivraison.CONFIRMEE
    );

    private final LivraisonRepository livraisonRepository;
    private final CloseurRepository closeurRepository;

    /** Closeur connecté (vide si l'action est faite par un admin). */
    private Closeur currentCloseur() {
        return closeurRepository.findByTelephone(SecurityUtils.getCurrentUserTelephone()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CommandeCloseur> getCommandes(
            sn.votreplateforme.logistique.dto.StatutLivraison statutDto,
            Integer page,
            Integer size
    ) {
        List<StatutLivraison> statuts = FILE_CLOSEUR;
        if (statutDto != null) {
            statuts = List.of(StatutLivraison.valueOf(statutDto.name()));
        }

        List<CommandeCloseur> commandes = livraisonRepository
                .findByStatutInOrderByDateCreationAsc(statuts)
                .stream()
                .map(this::mapToCommandeCloseur)
                .collect(Collectors.toList());

        // Pagination simple côté mémoire (les files closeur restent petites)
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 200) ? size : 50;
        int from = Math.min(pageNumber * pageSize, commandes.size());
        int to = Math.min(from + pageSize, commandes.size());

        log.info("File closeur : {} commande(s) (statuts={})", commandes.size(), statuts);
        return commandes.subList(from, to);
    }

    @Transactional
    public CommandeCloseur appeler(Long id) {
        Livraison l = getCommande(id);
        exigerStatut(l, StatutLivraison.NOUVELLE);
        l.marquerAAppeler();
        if (l.getCloseur() == null) {
            l.setCloseur(currentCloseur());
        }
        return mapToCommandeCloseur(livraisonRepository.save(l));
    }

    @Transactional
    public CommandeCloseur confirmer(Long id) {
        Livraison l = getCommande(id);
        if (l.getStatut() != StatutLivraison.NOUVELLE && l.getStatut() != StatutLivraison.A_APPELER) {
            throw new BusinessException("Seule une commande Nouvelle ou A appeler peut être confirmée");
        }
        l.marquerConfirmee();
        if (l.getCloseur() == null) {
            l.setCloseur(currentCloseur());
        }
        log.info("Commande {} confirmée par le closeur", l.getNumeroTracking());
        return mapToCommandeCloseur(livraisonRepository.save(l));
    }

    @Transactional
    public CommandeCloseur preteALivrer(Long id) {
        Livraison l = getCommande(id);
        exigerStatut(l, StatutLivraison.CONFIRMEE);
        l.marquerPreteALivrer();
        log.info("Commande {} prête à livrer (dispatch)", l.getNumeroTracking());
        return mapToCommandeCloseur(livraisonRepository.save(l));
    }

    /**
     * "Relancer" : la commande retourne dans la file en tant que NOUVELLE,
     * redevenant disponible pour prise en charge par n'importe quel closeur.
     */
    @Transactional
    public CommandeCloseur reporter(Long id, CommentaireRequest request) {
        Livraison l = getCommande(id);
        if (l.estTerminee() || l.getStatut() == StatutLivraison.PRETE_A_LIVRER) {
            throw new BusinessException("Cette commande ne peut plus être relancée");
        }
        l.setStatut(StatutLivraison.NOUVELLE);
        if (request != null && request.getCommentaire() != null) {
            l.setCommentaireLivraison(request.getCommentaire());
        }
        log.info("Commande {} relancée : de nouveau disponible pour prise en charge", l.getNumeroTracking());
        return mapToCommandeCloseur(livraisonRepository.save(l));
    }

    @Transactional
    public CommandeCloseur annuler(Long id, CommentaireRequest request) {
        Livraison l = getCommande(id);
        if (l.estTerminee()) {
            throw new BusinessException("Cette commande est déjà terminée et ne peut pas être annulée");
        }
        l.annuler(request != null ? request.getCommentaire() : null);
        log.info("Commande {} annulée par le closeur", l.getNumeroTracking());
        return mapToCommandeCloseur(livraisonRepository.save(l));
    }

    // ==================== HELPERS ====================

    private Livraison getCommande(Long id) {
        return livraisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + id));
    }

    private void exigerStatut(Livraison l, StatutLivraison attendu) {
        if (l.getStatut() != attendu) {
            throw new BusinessException(
                    "Action impossible : la commande doit être au statut " + attendu
                            + " (actuel: " + l.getStatut() + ")");
        }
    }

    private CommandeCloseur mapToCommandeCloseur(Livraison l) {
        CommandeCloseur c = new CommandeCloseur();
        c.setId(l.getId());
        c.setNumeroTracking(l.getNumeroTracking());
        c.setStatut(sn.votreplateforme.logistique.dto.StatutLivraison.valueOf(l.getStatut().name()));
        c.setNomClient(l.getNomClient());
        c.setTelephoneClient(l.getTelephoneClient());
        if (l.getAdresseDestination() != null) {
            c.setAdresse(l.getAdresseDestination().getAdresseComplete());
            if (l.getAdresseDestination().getZone() != null) {
                c.setZone(l.getAdresseDestination().getZone().getNom());
            }
        }
        c.setProduit(l.getDescriptionProduit());
        c.setMontantCOD(l.getMontantCOD());
        if (l.getDateCreation() != null) {
            c.setDateCreation(l.getDateCreation().atOffset(ZoneOffset.UTC));
        }
        return c;
    }
}
