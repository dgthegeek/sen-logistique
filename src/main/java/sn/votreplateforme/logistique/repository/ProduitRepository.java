package sn.votreplateforme.logistique.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Produit;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Produit (module Stock).
 */
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    Optional<Produit> findByCode(String code);

    boolean existsByCode(String code);

    long countByCodeStartingWith(String prefix);

    List<Produit> findByVendeurIdOrderByNomAsc(Long vendeurId);

    Page<Produit> findByVendeurId(Long vendeurId, Pageable pageable);

    /** Produits en alerte (stock <= seuil). */
    @Query("SELECT p FROM Produit p WHERE p.quantiteStock <= p.seuilAlerte ORDER BY p.quantiteStock ASC")
    List<Produit> findEnAlerte();

    @Query("SELECT p FROM Produit p WHERE " +
            "(:search IS NULL OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.dateCreation DESC")
    Page<Produit> rechercher(@Param("search") String search, Pageable pageable);
}
