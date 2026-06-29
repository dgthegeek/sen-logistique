package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sn.votreplateforme.logistique.dto.TrackingResponse;
import sn.votreplateforme.logistique.dto.TrackingResponseTimelineInner;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service Tracking - Suivi public des colis
 *
 * ResponsabilitÃ©s :
 * - Fournir les informations de tracking publiques
 * - Construire la timeline du colis
 * - Mapper les statuts vers les Ã©tapes de la timeline
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrackingService {

    private final LivraisonRepository livraisonRepository;

    /**
     * RÃ©cupÃ¨re les informations de tracking public d'un colis
     *
     * @param numeroTracking Numero de tracking
     * @return TrackingResponse avec timeline et statut actuel
     */
    public TrackingResponse getTrackingInfo(String numeroTracking) {
        log.info("Consultation tracking public pour: {}", numeroTracking);

        // 1. RÃ©cupÃ©rer la livraison
        Livraison livraison = livraisonRepository.findByNumeroTracking(numeroTracking)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Colis non trouvee: " + numeroTracking
                ));

        // 2. Construire la repponse selon le schema OpenAPI
        TrackingResponse response = new TrackingResponse();
        response.setNumeroTracking(numeroTracking);
        response.setStatut(sn.votreplateforme.logistique.dto.StatutLivraison.valueOf(String.valueOf(livraison.getStatut())));
        response.setDestination(livraison.getAdresseDestination().getQuartier());
        response.setMontantAPayer(livraison.getMontantCOD());

        // 3. Construire la timeline
        List<TrackingResponseTimelineInner> timeline = construireTimeline(livraison);
        response.setTimeline(timeline);

        log.info("Tracking {} recuperer - Statut: {}, Destination: {}",
                numeroTracking,
                livraison.getStatut(),
                livraison.getAdresseDestination().getQuartier()
        );

        return response;
    }

    /**
     * Construit la timeline du colis Ã  partir de son Ã©tat actuel
     *
     * @param livraison La livraison
     * @return Liste des Ã©tapes de la timeline
     */
    private List<TrackingResponseTimelineInner> construireTimeline(Livraison livraison) {
        List<TrackingResponseTimelineInner> timeline = new ArrayList<>();

        // Ã‰tape 1 : COMMANDE_CREEE (toujours effectuÃ©e)
        TrackingResponseTimelineInner etape1 = new TrackingResponseTimelineInner();
        etape1.setEtape(TrackingResponseTimelineInner.EtapeEnum.COMMANDE_CREEE);
        etape1.setDate(convertToOffsetDateTime(livraison.getDateCreation()));
        etape1.setEffectue(true);
        timeline.add(etape1);

        // Étape 2 : Commande confirmée / préparée (nouveau cycle)
        TrackingResponseTimelineInner etape2 = new TrackingResponseTimelineInner();
        etape2.setEtape(TrackingResponseTimelineInner.EtapeEnum.COLIS_RECUPERE);

        boolean confirmee = livraison.getStatut() == StatutLivraison.CONFIRMEE
                || livraison.getStatut() == StatutLivraison.PRETE_A_LIVRER
                || livraison.getStatut() == StatutLivraison.ASSIGNEE
                || livraison.getStatut() == StatutLivraison.EN_LIVRAISON
                || livraison.getStatut() == StatutLivraison.LIVREE
                // anciens statuts (dormant)
                || livraison.getDateRamassage() != null;
        if (confirmee) {
            if (livraison.getDateConfirmation() != null) {
                etape2.setDate(convertToOffsetDateTime(livraison.getDateConfirmation()));
            } else if (livraison.getDateRamassage() != null) {
                etape2.setDate(convertToOffsetDateTime(livraison.getDateRamassage()));
            }
            etape2.setEffectue(true);
        } else {
            etape2.setEffectue(false);
        }
        timeline.add(etape2);

        // Étape 3 : EN_COURS_LIVRAISON (assignée / en livraison)
        TrackingResponseTimelineInner etape3 = new TrackingResponseTimelineInner();
        etape3.setEtape(TrackingResponseTimelineInner.EtapeEnum.EN_COURS_LIVRAISON);

        if (livraison.getStatut() == StatutLivraison.ASSIGNEE
                || livraison.getStatut() == StatutLivraison.EN_LIVRAISON
                || livraison.getStatut() == StatutLivraison.LIVREE
                || livraison.getStatut() == StatutLivraison.RAMASSE
                || livraison.getStatut() == StatutLivraison.EN_ROUTE) {
            if (livraison.getDateEnRoute() != null) {
                etape3.setDate(convertToOffsetDateTime(livraison.getDateEnRoute()));
            }
            etape3.setEffectue(true);
        } else {
            etape3.setEffectue(false);
        }
        timeline.add(etape3);

        // Ã‰tape 4 : LIVRE (si livrÃ©)
        TrackingResponseTimelineInner etape4 = new TrackingResponseTimelineInner();
        etape4.setEtape(TrackingResponseTimelineInner.EtapeEnum.LIVRE);

        if (livraison.getDateLivraison() != null) {
            etape4.setDate(convertToOffsetDateTime(livraison.getDateLivraison()));
            etape4.setEffectue(true);
        } else {
            etape4.setEffectue(false);
        }
        timeline.add(etape4);

        return timeline;
    }

    /**
     * Convertit LocalDateTime en OffsetDateTime pour l'API
     *
     * @param localDateTime Date locale
     * @return OffsetDateTime avec timezone
     */
    private OffsetDateTime convertToOffsetDateTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}