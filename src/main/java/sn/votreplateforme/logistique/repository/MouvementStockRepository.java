package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.MouvementStock;
import sn.votreplateforme.logistique.entity.TypeMouvement;

import java.util.List;

/**
 * Repository pour le journal des mouvements de stock.
 */
@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(Long produitId);

    /** Somme des variations d'un type donné pour un produit (ex: total entrées/sorties). */
    long countByProduitIdAndType(Long produitId, TypeMouvement type);
}
