package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.AssignerLivreurRequest;
import sn.votreplateforme.logistique.dto.CommandeDispatch;
import sn.votreplateforme.logistique.dto.DispatchAssigner200Response;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.Livreur;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.exception.BusinessException;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.LivreurRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service du module Dispatch.
 *
 * L'administrateur attribue les commandes "Prête à livrer" à un livreur.
 * Seules ces commandes peuvent être assignées.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DispatchService {

    private final LivraisonRepository livraisonRepository;
    private final LivreurRepository livreurRepository;

    @Transactional(readOnly = true)
    public List<CommandeDispatch> getPretes() {
        List<CommandeDispatch> commandes = livraisonRepository
                .findByStatutOrderByDateCreationAsc(StatutLivraison.PRETE_A_LIVRER)
                .stream()
                .map(this::mapToCommandeDispatch)
                .collect(Collectors.toList());
        log.info("Dispatch : {} commande(s) prête(s) à livrer", commandes.size());
        return commandes;
    }

    @Transactional
    public DispatchAssigner200Response assigner(AssignerLivreurRequest request) {
        if (request.getLivraisonIds() == null || request.getLivraisonIds().isEmpty()) {
            throw new BusinessException("Aucune commande sélectionnée");
        }

        Livreur livreur = livreurRepository.findById(request.getLivreurId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Livreur non trouvé: " + request.getLivreurId()));

        if (!livreur.isActif()) {
            throw new BusinessException("Ce livreur est désactivé");
        }

        int assignees = 0;
        for (Long id : request.getLivraisonIds()) {
            Livraison l = livraisonRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + id));

            if (l.getStatut() != StatutLivraison.PRETE_A_LIVRER) {
                throw new BusinessException(
                        "La commande " + l.getNumeroTracking()
                                + " n'est pas prête à livrer (statut: " + l.getStatut() + ")");
            }

            l.assignerLivreur(livreur);
            livraisonRepository.save(l);
            assignees++;
        }

        log.info("{} commande(s) assignée(s) au livreur {}", assignees, livreur.getNomComplet());

        DispatchAssigner200Response response = new DispatchAssigner200Response();
        response.setNombreAssignees(assignees);
        response.setMessage(assignees + " commande(s) assignée(s) à " + livreur.getNomComplet());
        return response;
    }

    // ==================== HELPERS ====================

    private CommandeDispatch mapToCommandeDispatch(Livraison l) {
        CommandeDispatch c = new CommandeDispatch();
        c.setId(l.getId());
        c.setNumeroTracking(l.getNumeroTracking());
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
        return c;
    }
}
