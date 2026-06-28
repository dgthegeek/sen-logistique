package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminDispatchApi;
import sn.votreplateforme.logistique.dto.AssignerLivreurRequest;
import sn.votreplateforme.logistique.dto.CommandeDispatch;
import sn.votreplateforme.logistique.dto.DispatchAssigner200Response;
import sn.votreplateforme.logistique.service.DispatchService;

import java.util.List;

/**
 * Controller du module Dispatch (attribution des livraisons aux livreurs).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminDispatchController implements AdminDispatchApi {

    private final DispatchService dispatchService;

    @Override
    public ResponseEntity<List<CommandeDispatch>> dispatchPretes() {
        return ResponseEntity.ok(dispatchService.getPretes());
    }

    @Override
    public ResponseEntity<DispatchAssigner200Response> dispatchAssigner(AssignerLivreurRequest assignerLivreurRequest) {
        return ResponseEntity.ok(dispatchService.assigner(assignerLivreurRequest));
    }
}
