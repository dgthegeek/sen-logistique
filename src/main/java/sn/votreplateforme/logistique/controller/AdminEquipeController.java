package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminEquipeApi;
import sn.votreplateforme.logistique.dto.CreateMembreRequest;
import sn.votreplateforme.logistique.dto.LivreurResponse;
import sn.votreplateforme.logistique.dto.MembreResponse;
import sn.votreplateforme.logistique.dto.UpdateMembreRequest;
import sn.votreplateforme.logistique.service.EquipeService;

import java.util.List;

/**
 * Controller de gestion de l'équipe (closeurs et livreurs). Réservé à l'admin.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminEquipeController implements AdminEquipeApi {

    private final EquipeService equipeService;

    @Override
    public ResponseEntity<MembreResponse> createCloseur(CreateMembreRequest createMembreRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.createCloseur(createMembreRequest));
    }

    @Override
    public ResponseEntity<List<MembreResponse>> listCloseurs() {
        return ResponseEntity.ok(equipeService.listCloseurs());
    }

    @Override
    public ResponseEntity<LivreurResponse> createLivreur(CreateMembreRequest createMembreRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.createLivreur(createMembreRequest));
    }

    @Override
    public ResponseEntity<List<LivreurResponse>> listLivreurs() {
        return ResponseEntity.ok(equipeService.listLivreurs());
    }

    @Override
    public ResponseEntity<MembreResponse> updateCloseur(Long id, UpdateMembreRequest updateMembreRequest) {
        return ResponseEntity.ok(equipeService.updateCloseur(id, updateMembreRequest));
    }

    @Override
    public ResponseEntity<LivreurResponse> updateLivreur(Long id, UpdateMembreRequest updateMembreRequest) {
        return ResponseEntity.ok(equipeService.updateLivreur(id, updateMembreRequest));
    }
}
