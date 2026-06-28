package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.LivreurApi;
import sn.votreplateforme.logistique.dto.CommandeLivreur;
import sn.votreplateforme.logistique.dto.EchecRequest;
import sn.votreplateforme.logistique.dto.LivrerRequest;
import sn.votreplateforme.logistique.dto.StatutLivraison;
import sn.votreplateforme.logistique.service.LivreurService;

import java.util.List;

/**
 * Controller de l'interface livreur ("Mes livraisons").
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class LivreurController implements LivreurApi {

    private final LivreurService livreurService;

    @Override
    public ResponseEntity<List<CommandeLivreur>> mesLivraisons(StatutLivraison statut) {
        return ResponseEntity.ok(livreurService.mesLivraisons(statut));
    }

    @Override
    public ResponseEntity<CommandeLivreur> livreurCommencer(Long id) {
        return ResponseEntity.ok(livreurService.commencer(id));
    }

    @Override
    public ResponseEntity<CommandeLivreur> livreurLivrer(Long id, LivrerRequest livrerRequest) {
        return ResponseEntity.ok(livreurService.livrer(id, livrerRequest));
    }

    @Override
    public ResponseEntity<CommandeLivreur> livreurEchec(Long id, EchecRequest echecRequest) {
        return ResponseEntity.ok(livreurService.echec(id, echecRequest));
    }
}
