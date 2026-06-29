package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminStatsApi;
import sn.votreplateforme.logistique.dto.BilanJour;
import sn.votreplateforme.logistique.dto.DashboardStats;
import sn.votreplateforme.logistique.service.BilanService;
import sn.votreplateforme.logistique.service.StatsService;

import java.time.LocalDate;

/**
 * Controller du tableau de bord / statistiques (admin).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminStatsController implements AdminStatsApi {

    private final StatsService statsService;
    private final BilanService bilanService;

    @Override
    public ResponseEntity<DashboardStats> statsDashboard() {
        return ResponseEntity.ok(statsService.getDashboard());
    }

    @Override
    public ResponseEntity<BilanJour> bilanJour(LocalDate date) {
        return ResponseEntity.ok(bilanService.getBilan(date));
    }
}
