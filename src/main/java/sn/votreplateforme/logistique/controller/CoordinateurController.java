package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.CoordinateurApi;
import sn.votreplateforme.logistique.dto.CreateLivraisonRequest;
import sn.votreplateforme.logistique.dto.LivraisonResponse;
import sn.votreplateforme.logistique.dto.LivreurSolde;
import sn.votreplateforme.logistique.dto.PageLivraison;
import sn.votreplateforme.logistique.dto.PageVersement;
import sn.votreplateforme.logistique.dto.StatutLivraison;
import sn.votreplateforme.logistique.dto.VerserLivreurRequest;
import sn.votreplateforme.logistique.dto.VersementLivreur;
import sn.votreplateforme.logistique.service.AdminLivraisonService;
import sn.votreplateforme.logistique.service.LivraisonService;
import sn.votreplateforme.logistique.service.VersementLivreurService;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller du Coordinateur Logistique (ex-Dispatcheur).
 *
 * <p>Au-delà du dispatch (assignation aux livreurs), le coordinateur peut :
 * <ul>
 *   <li>consulter l'historique des commandes (filtrable) ;</li>
 *   <li>créer une commande pour un vendeur ;</li>
 *   <li>gérer la finance des livreurs : voir le cash à reverser par livreur et
 *       le marquer comme versé (remise du solde à zéro + historique).</li>
 * </ul>
 * Les endpoints sont sous {@code /dispatch/**} : accessibles DISPATCHEUR + ADMIN.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CoordinateurController implements CoordinateurApi {

    private final AdminLivraisonService adminLivraisonService;
    private final LivraisonService livraisonService;
    private final VersementLivreurService versementLivreurService;

    @Override
    public ResponseEntity<PageLivraison> coordinateurHistorique(
            StatutLivraison statut, LocalDate date, Integer page, Integer size) {
        return ResponseEntity.ok(adminLivraisonService.getAllLivraisons(statut, date, page, size));
    }

    @Override
    public ResponseEntity<LivraisonResponse> coordinateurCreerCommande(
            CreateLivraisonRequest createLivraisonRequest) {
        LivraisonResponse response = livraisonService.creerLivraison(createLivraisonRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<LivreurSolde>> coordinateurSoldesLivreurs() {
        return ResponseEntity.ok(versementLivreurService.soldesLivreurs());
    }

    @Override
    public ResponseEntity<VersementLivreur> coordinateurVerserLivreur(
            Long livreurId, VerserLivreurRequest verserLivreurRequest) {
        return ResponseEntity.ok(versementLivreurService.verserLivreur(livreurId, verserLivreurRequest));
    }

    @Override
    public ResponseEntity<PageVersement> coordinateurVersements(Integer page, Integer size) {
        return ResponseEntity.ok(versementLivreurService.historique(page, size));
    }
}
