package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.CloseurApi;
import sn.votreplateforme.logistique.dto.CommandeCloseur;
import sn.votreplateforme.logistique.dto.CommentaireRequest;
import sn.votreplateforme.logistique.dto.StatutLivraison;
import sn.votreplateforme.logistique.service.ClosingService;

import java.util.List;

/**
 * Controller du module Closing (file du closeur).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CloseurController implements CloseurApi {

    private final ClosingService closingService;

    @Override
    public ResponseEntity<List<CommandeCloseur>> closeurCommandesGet(
            StatutLivraison statut, Integer page, Integer size) {
        return ResponseEntity.ok(closingService.getCommandes(statut, page, size));
    }

    @Override
    public ResponseEntity<CommandeCloseur> closeurAppeler(Long id) {
        return ResponseEntity.ok(closingService.appeler(id));
    }

    @Override
    public ResponseEntity<CommandeCloseur> closeurConfirmer(Long id) {
        return ResponseEntity.ok(closingService.confirmer(id));
    }

    @Override
    public ResponseEntity<CommandeCloseur> closeurPreteALivrer(Long id) {
        return ResponseEntity.ok(closingService.preteALivrer(id));
    }

    @Override
    public ResponseEntity<CommandeCloseur> closeurReporter(Long id, CommentaireRequest commentaireRequest) {
        return ResponseEntity.ok(closingService.reporter(id, commentaireRequest));
    }

    @Override
    public ResponseEntity<CommandeCloseur> closeurAnnuler(Long id, CommentaireRequest commentaireRequest) {
        return ResponseEntity.ok(closingService.annuler(id, commentaireRequest));
    }

    @Override
    public ResponseEntity<List<CommandeCloseur>> closeurHistorique() {
        return ResponseEntity.ok(closingService.historique());
    }
}
