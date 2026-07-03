package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.CommandesApi;
import sn.votreplateforme.logistique.dto.LivraisonDetailResponse;
import sn.votreplateforme.logistique.service.VendeurService;

/**
 * Détail d'une commande partagé par le staff (closeur, dispatcheur, livreur, admin).
 * Réutilise la construction de détail (avec la traçabilité qualité).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CommandeController implements CommandesApi {

    private final VendeurService vendeurService;

    @Override
    public ResponseEntity<LivraisonDetailResponse> commandeDetail(Long id) {
        return ResponseEntity.ok(vendeurService.getDetailLivraison(id));
    }
}
