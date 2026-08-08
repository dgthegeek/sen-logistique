package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminMaintenanceApi;
import sn.votreplateforme.logistique.dto.MaintenanceResult;
import sn.votreplateforme.logistique.dto.SuppressionVendeurResult;
import sn.votreplateforme.logistique.dto.VendeurImpact;
import sn.votreplateforme.logistique.service.MaintenanceService;

/**
 * Suppression de données de test (réservé ADMIN — chemin /admin/**).
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminMaintenanceController implements AdminMaintenanceApi {

    private final MaintenanceService maintenanceService;

    @Override
    public ResponseEntity<MaintenanceResult> maintenanceSupprimerLivraison(Long id) {
        MaintenanceResult r = new MaintenanceResult();
        r.setMessage(maintenanceService.supprimerLivraison(id));
        return ResponseEntity.ok(r);
    }

    @Override
    public ResponseEntity<MaintenanceResult> maintenanceSupprimerTransaction(Long id) {
        MaintenanceResult r = new MaintenanceResult();
        r.setMessage(maintenanceService.supprimerTransaction(id));
        return ResponseEntity.ok(r);
    }

    @Override
    public ResponseEntity<MaintenanceResult> maintenanceSupprimerMembre(Long userId) {
        MaintenanceResult r = new MaintenanceResult();
        r.setMessage(maintenanceService.supprimerMembre(userId));
        return ResponseEntity.ok(r);
    }

    @Override
    public ResponseEntity<VendeurImpact> maintenanceImpactVendeur(Long id) {
        int[] impact = maintenanceService.impactVendeur(id);
        VendeurImpact dto = new VendeurImpact();
        dto.setVendeurId(id);
        dto.setNom(maintenanceService.vendeurNom(id));
        dto.setLivraisons(impact[0]);
        dto.setTransactions(impact[1]);
        dto.setProduits(impact[2]);
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<SuppressionVendeurResult> maintenanceSupprimerVendeur(Long id) {
        String nom = maintenanceService.vendeurNom(id);
        int[] res = maintenanceService.supprimerVendeur(id);
        SuppressionVendeurResult dto = new SuppressionVendeurResult();
        dto.setLivraisonsSupprimees(res[0]);
        dto.setTransactionsSupprimees(res[1]);
        dto.setProduitsSupprimes(res[2]);
        dto.setMessage("Vendeur " + nom + " supprimé (" + res[0] + " livraison(s), "
                + res[1] + " transaction(s), " + res[2] + " produit(s))");
        return ResponseEntity.ok(dto);
    }
}
