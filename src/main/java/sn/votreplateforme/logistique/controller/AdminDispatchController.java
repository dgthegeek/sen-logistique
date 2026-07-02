package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.DispatchApi;
import sn.votreplateforme.logistique.dto.AssignerLivreurRequest;
import sn.votreplateforme.logistique.dto.CommandeDispatch;
import sn.votreplateforme.logistique.dto.DispatchAssigner200Response;
import sn.votreplateforme.logistique.service.DispatchService;

import java.util.List;

/**
 * Controller du module Dispatch (attribution des livraisons aux livreurs).
 * Accessible au dispatcheur et à l'admin (voir SecurityConfig : /dispatch/**).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminDispatchController implements DispatchApi {

    private final DispatchService dispatchService;

    @Override
    public ResponseEntity<List<CommandeDispatch>> dispatchPretes() {
        return ResponseEntity.ok(dispatchService.getPretes());
    }

    @Override
    public ResponseEntity<List<sn.votreplateforme.logistique.dto.LivreurResponse>> dispatchLivreurs() {
        return ResponseEntity.ok(dispatchService.getLivreursActifs());
    }

    @Override
    public ResponseEntity<DispatchAssigner200Response> dispatchAssigner(AssignerLivreurRequest assignerLivreurRequest) {
        return ResponseEntity.ok(dispatchService.assigner(assignerLivreurRequest));
    }
}
