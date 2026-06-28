package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminStatsApi;
import sn.votreplateforme.logistique.dto.DashboardStats;
import sn.votreplateforme.logistique.service.StatsService;

/**
 * Controller du tableau de bord / statistiques (admin).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminStatsController implements AdminStatsApi {

    private final StatsService statsService;

    @Override
    public ResponseEntity<DashboardStats> statsDashboard() {
        return ResponseEntity.ok(statsService.getDashboard());
    }
}
