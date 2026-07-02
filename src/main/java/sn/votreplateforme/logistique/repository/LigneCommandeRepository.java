package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.LigneCommande;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour l'entité LigneCommande (produits vendus par livraison).
 */
@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {

    /**
     * Ventes par produit d'un vendeur sur une période : uniquement les livraisons LIVREE,
     * bornées par leur date de livraison.
     * Retourne des lignes [produitId (Long), quantiteVendue (Long), montantVentes (BigDecimal)].
     */
    @Query("SELECT lc.produit.id, COALESCE(SUM(lc.quantite), 0), COALESCE(SUM(lc.prixUnitaire * lc.quantite), 0) " +
            "FROM LigneCommande lc " +
            "WHERE lc.livraison.vendeur.id = :vendeurId " +
            "AND lc.livraison.statut = 'LIVREE' " +
            "AND lc.livraison.dateLivraison BETWEEN :debut AND :fin " +
            "GROUP BY lc.produit.id")
    List<Object[]> ventesParProduit(@Param("vendeurId") Long vendeurId,
                                    @Param("debut") LocalDateTime debut,
                                    @Param("fin") LocalDateTime fin);
}
