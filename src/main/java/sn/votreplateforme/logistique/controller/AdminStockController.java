package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminStockApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.StockService;

import java.util.List;

/**
 * Controller du module Stock (gestion des produits et du stock). Réservé à l'admin.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminStockController implements AdminStockApi {

    private final StockService stockService;

    @Override
    public ResponseEntity<PageProduit> listProduits(String search, Integer page, Integer size) {
        return ResponseEntity.ok(stockService.listProduits(search, page, size));
    }

    @Override
    public ResponseEntity<ProduitResponse> createProduit(CreateProduitRequest createProduitRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.createProduit(createProduitRequest));
    }

    @Override
    public ResponseEntity<ProduitResponse> getProduit(Long id) {
        return ResponseEntity.ok(stockService.getProduit(id));
    }

    @Override
    public ResponseEntity<ProduitResponse> updateProduit(Long id, UpdateProduitRequest updateProduitRequest) {
        return ResponseEntity.ok(stockService.updateProduit(id, updateProduitRequest));
    }

    @Override
    public ResponseEntity<ProduitResponse> entreeStock(Long id, MouvementStockRequest mouvementStockRequest) {
        return ResponseEntity.ok(stockService.entreeStock(id, mouvementStockRequest));
    }

    @Override
    public ResponseEntity<ProduitResponse> ajusterStock(Long id, AjustementStockRequest ajustementStockRequest) {
        return ResponseEntity.ok(stockService.ajusterStock(id, ajustementStockRequest));
    }

    @Override
    public ResponseEntity<List<MouvementResponse>> getMouvements(Long id) {
        return ResponseEntity.ok(stockService.getMouvements(id));
    }

    @Override
    public ResponseEntity<ProduitResponse> scanProduit(String code) {
        return ResponseEntity.ok(stockService.scanProduit(code));
    }

    @Override
    public ResponseEntity<List<ProduitResponse>> stockAlertes() {
        return ResponseEntity.ok(stockService.getAlertes());
    }
}
