package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminPerformanceApi;
import sn.votreplateforme.logistique.dto.PerformanceResponse;
import sn.votreplateforme.logistique.service.PerformanceService;

/**
 * Contrôle qualité : performances de l'équipe (réservé à l'admin).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminPerformanceController implements AdminPerformanceApi {

    private final PerformanceService performanceService;

    @Override
    public ResponseEntity<PerformanceResponse> adminPerformance(
            String periode, java.time.LocalDate debut, java.time.LocalDate fin) {
        return ResponseEntity.ok(performanceService.getPerformance(periode, debut, fin));
    }
}
