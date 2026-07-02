package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.BilanVendeur;
import sn.votreplateforme.logistique.dto.BilanVendeurLigne;
import sn.votreplateforme.logistique.dto.BilanVendeurVendeur;
import sn.votreplateforme.logistique.entity.Produit;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.repository.LigneCommandeRepository;
import sn.votreplateforme.logistique.repository.ProduitRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bilan d'un vendeur : pour chaque produit, le stock actuel chez Dioks et les ventes
 * (livraisons livrées) sur une période. Utilisé côté vendeur et côté admin.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BilanVendeurService {

    private final ProduitRepository produitRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final VendeurRepository vendeurRepository;

    /** Bilan du vendeur connecté. */
    @Transactional(readOnly = true)
    public BilanVendeur getBilanVendeurConnecte(LocalDate debut, LocalDate fin) {
        String telephone = SecurityUtils.getCurrentUserTelephone();
        Vendeur vendeur = vendeurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new IllegalStateException("Vendeur non trouvé"));
        return construire(vendeur, debut, fin);
    }

    /** Bilan d'un vendeur donné (accès admin). */
    @Transactional(readOnly = true)
    public BilanVendeur getBilanVendeur(Long vendeurId, LocalDate debut, LocalDate fin) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new sn.votreplateforme.logistique.exception.ResourceNotFoundException(
                        "Vendeur non trouvé: " + vendeurId));
        return construire(vendeur, debut, fin);
    }

    // ==================== CONSTRUCTION ====================

    private BilanVendeur construire(Vendeur vendeur, LocalDate debutParam, LocalDate finParam) {
        // Période : défaut = aujourd'hui
        LocalDate debut = debutParam != null ? debutParam : LocalDate.now();
        LocalDate fin = finParam != null ? finParam : debut;
        if (fin.isBefore(debut)) {
            LocalDate tmp = debut;
            debut = fin;
            fin = tmp;
        }
        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT = fin.atTime(23, 59, 59);

        // Ventes par produit (livraisons livrées sur la période)
        Map<Long, long[]> qteParProduit = new HashMap<>();
        Map<Long, BigDecimal> montantParProduit = new HashMap<>();
        for (Object[] row : ligneCommandeRepository.ventesParProduit(vendeur.getId(), debutDT, finDT)) {
            Long produitId = ((Number) row[0]).longValue();
            long qte = ((Number) row[1]).longValue();
            BigDecimal montant = (BigDecimal) row[2];
            qteParProduit.put(produitId, new long[]{qte});
            montantParProduit.put(produitId, montant != null ? montant : BigDecimal.ZERO);
        }

        List<Produit> produits = produitRepository.findByVendeurIdOrderByNomAsc(vendeur.getId());

        BilanVendeur bilan = new BilanVendeur();

        BilanVendeurVendeur v = new BilanVendeurVendeur();
        v.setId(vendeur.getId());
        v.setNom(vendeur.getNom());
        v.setPrenom(vendeur.getPrenom());
        v.setNomBoutique(vendeur.getNomBoutique());
        v.setTelephone(vendeur.getTelephone());
        bilan.setVendeur(v);

        bilan.setPeriodeDebut(debut);
        bilan.setPeriodeFin(fin);
        bilan.setGenereLe(OffsetDateTime.now(ZoneOffset.UTC));

        int totalStock = 0;
        long totalQuantiteVendue = 0;
        BigDecimal totalMontant = BigDecimal.ZERO;

        for (Produit p : produits) {
            int stock = p.getQuantiteStock() != null ? p.getQuantiteStock() : 0;
            long qteVendue = qteParProduit.containsKey(p.getId()) ? qteParProduit.get(p.getId())[0] : 0;
            BigDecimal montant = montantParProduit.getOrDefault(p.getId(), BigDecimal.ZERO);
            boolean enAlerte = p.getSeuilAlerte() != null && stock <= p.getSeuilAlerte();

            BilanVendeurLigne ligne = new BilanVendeurLigne();
            ligne.setProduitId(p.getId());
            ligne.setCode(p.getCode());
            ligne.setNom(p.getNom());
            ligne.setPrixUnitaire(p.getPrixUnitaire());
            ligne.setStockActuel(stock);
            ligne.setQuantiteVendue((int) qteVendue);
            ligne.setMontantVentes(montant);
            ligne.setEnAlerte(enAlerte);
            bilan.addLignesItem(ligne);

            totalStock += stock;
            totalQuantiteVendue += qteVendue;
            totalMontant = totalMontant.add(montant);
        }

        bilan.setNombreProduits(produits.size());
        bilan.setTotalStock(totalStock);
        bilan.setTotalQuantiteVendue((int) totalQuantiteVendue);
        bilan.setTotalMontantVentes(totalMontant);

        log.info("Bilan vendeur {} - {} produit(s), {} vendu(s), {} FCFA de ventes ({} → {})",
                vendeur.getId(), produits.size(), totalQuantiteVendue, totalMontant, debut, fin);

        return bilan;
    }
}
