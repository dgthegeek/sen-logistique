package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminFinancesApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.FinanceService;
import sn.votreplateforme.logistique.service.TransactionService;
import sn.votreplateforme.logistique.service.VersementLivreurService;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller Admin Finances - Gestion financière
 * 
 * Implémente l'interface AdminFinancesApi générée par OpenAPI
 * 
 * Endpoints protégés (ROLE_ADMIN requis) :
 * - GET  /admin/finances/dashboard           - Dashboard financier
 * - GET  /admin/finances/paiements-pending   - Demandes de paiement
 * - POST /admin/finances/payer-vendeur/{id}  - Payer un vendeur
 * - GET  /admin/finances/transactions        - Historique transactions
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminFinanceController implements AdminFinancesApi {
    
    private final FinanceService financeService;
    private final TransactionService transactionService;
    private final VersementLivreurService versementLivreurService;
    
    /**
     * GET /admin/finances/dashboard
     * 
     * Dashboard financier avec statistiques globales.
     * 
     * @param periode Période : jour, semaine, mois, tout (défaut: jour)
     * @return AdminFinancesDashboard avec stats
     */
    @Override
    public ResponseEntity<AdminFinancesDashboard> adminFinancesDashboardGet(String periode) {
        log.info("💰 Dashboard financier demandé - Période: {}", 
            periode != null ? periode : "jour");
        
        String periodeEffective = periode != null ? periode : "jour";
        AdminFinancesDashboard dashboard = financeService.getDashboardFinancier(periodeEffective);
        
        log.info("✅ Dashboard généré - Cash: {} FCFA", dashboard.getCashCollecte());
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * GET /admin/finances/paiements-pending
     * 
     * Liste des vendeurs ayant un solde en attente de paiement.
     * 
     * @return AdminFinancesPaiementsPendingGet200Response avec demandes
     */
    @Override
    public ResponseEntity<AdminFinancesPaiementsPendingGet200Response> adminFinancesPaiementsPendingGet() {
        log.info("💰 Récupération demandes de paiement en attente");
        
        AdminFinancesPaiementsPendingGet200Response response = 
            financeService.getDemandesPaiementEnAttente();
        
        log.info("✅ {} demandes de paiement - Total: {} FCFA", 
            response.getDemandes().size(),
            response.getTotalAPayer()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /admin/finances/payer-vendeur/{vendeurId}
     * 
     * Effectue un paiement en cash à un vendeur.
     * Met à jour le solde du vendeur et crée une transaction.
     * 
     * @param vendeurId ID du vendeur
     * @param request Montant et commentaire
     * @return AdminFinancesPayerVendeurVendeurIdPost200Response avec confirmation
     */
    @Override
    public ResponseEntity<AdminFinancesPayerVendeurVendeurIdPost200Response> adminFinancesPayerVendeurVendeurIdPost(
        Long vendeurId,
        AdminFinancesPayerVendeurVendeurIdPostRequest request
    ) {
        log.info("💰 Paiement vendeur {} - Montant: {} FCFA", 
            vendeurId, 
            request.getMontant()
        );
        
        AdminFinancesPayerVendeurVendeurIdPost200Response response = 
            transactionService.payerVendeur(vendeurId, request);
        
        log.info("✅ Paiement effectué - Ref: {}", response.getReference());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /admin/finances/transactions
     * 
     * Historique des transactions avec pagination et filtres.
     * 
     * @param vendeurId Filtre par vendeur (optionnel)
     * @param dateDebut Filtre date début (optionnel)
     * @param dateFin Filtre date fin (optionnel)
     * @param page Numéro de page (défaut: 0)
     * @param size Taille de page (défaut: 50)
     * @return PageTransaction avec historique
     */
    @Override
    public ResponseEntity<PageTransaction> adminFinancesTransactionsGet(
        Long vendeurId,
        LocalDate dateDebut,
        LocalDate dateFin,
        Integer page,
        Integer size
    ) {
        log.info("💰 Historique transactions - VendeurId: {}, Page: {}", vendeurId, page);
        
        PageTransaction response = transactionService.getHistoriqueTransactions(
            vendeurId,
            dateDebut,
            dateFin,
            page,
            size
        );
        
        log.info("✅ Historique récupéré - {} transactions sur {} pages",
            response.getContent().size(),
            response.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    // ==================== FINANCES LIVREURS (cash COD à reverser) ====================

    /** GET /admin/finances/livreurs — soldes de cash à régler par livreur. */
    @Override
    public ResponseEntity<List<LivreurSolde>> adminSoldesLivreurs() {
        return ResponseEntity.ok(versementLivreurService.soldesLivreurs());
    }

    /** GET /admin/finances/partenaires — soldes à verser à chaque vendeur. */
    @Override
    public ResponseEntity<List<PartenaireSolde>> adminPartenairesSoldes() {
        return ResponseEntity.ok(financeService.getSoldesPartenaires());
    }

    /** POST /admin/finances/livreurs/{livreurId}/verser — remet le solde du livreur à zéro. */
    @Override
    public ResponseEntity<VersementLivreur> adminVerserLivreur(
            Long livreurId, VerserLivreurRequest verserLivreurRequest) {
        return ResponseEntity.ok(versementLivreurService.verserLivreur(livreurId, verserLivreurRequest));
    }

    /** GET /admin/finances/versements — historique des versements. */
    @Override
    public ResponseEntity<PageVersement> adminVersementsLivreur(Integer page, Integer size) {
        return ResponseEntity.ok(versementLivreurService.historique(page, size));
    }
}
