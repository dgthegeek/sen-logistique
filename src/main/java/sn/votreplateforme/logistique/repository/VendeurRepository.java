package sn.votreplateforme.logistique.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.dto.StatutVendeur;
import sn.votreplateforme.logistique.entity.Vendeur;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendeurRepository extends JpaRepository<Vendeur, Long> {

    Optional<Vendeur> findByTelephone(String telephone);

    List<Vendeur> findBySoldeEnAttenteGreaterThan(BigDecimal montant);

    List<Vendeur> findByStatut(StatutVendeur statut);

    /**
     * Recherche avec ID uniquement (évite le problème password)
     */
    @Query(value = "SELECT v.id FROM vendeurs v " +
            "JOIN users u ON v.id = u.id " +
            "WHERE (:statut IS NULL OR v.statut = CAST(:statut AS VARCHAR)) " +
            "AND (:quartier IS NULL OR LOWER(v.quartier) LIKE LOWER('%' || :quartier || '%')) " +
            "AND (:commune IS NULL OR LOWER(v.commune) LIKE LOWER('%' || :commune || '%')) " +
            "AND (:search IS NULL OR " +
            "     LOWER(u.nom) LIKE LOWER('%' || :search || '%') OR " +
            "     LOWER(u.prenom) LIKE LOWER('%' || :search || '%') OR " +
            "     LOWER(u.telephone) LIKE LOWER('%' || :search || '%') OR " +
            "     LOWER(v.nom_boutique) LIKE LOWER('%' || :search || '%')) " +
            "ORDER BY u.date_inscription DESC",
            nativeQuery = true)
    List<Long> searchVendeurIds(
            @Param("statut") String statut,
            @Param("quartier") String quartier,
            @Param("commune") String commune,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(value = "SELECT COUNT(v.id) FROM vendeurs v " +
            "JOIN users u ON v.id = u.id " +
            "WHERE (:statut IS NULL OR v.statut = CAST(:statut AS VARCHAR)) " +
            "AND (:quartier IS NULL OR LOWER(v.quartier) LIKE LOWER('%' || :quartier || '%')) " +
            "AND (:commune IS NULL OR LOWER(v.commune) LIKE LOWER('%' || :commune || '%')) " +
            "AND (:search IS NULL OR " +
            "     LOWER(u.nom) LIKE LOWER('%' || :search || '%') OR " +
            "     LOWER(u.prenom) LIKE LOWER('%' || :search || '%') OR " +
            "     LOWER(u.telephone) LIKE LOWER('%' || :search || '%') OR " +
            "     LOWER(v.nom_boutique) LIKE LOWER('%' || :search || '%'))",
            nativeQuery = true)
    long countVendeurs(
            @Param("statut") String statut,
            @Param("quartier") String quartier,
            @Param("commune") String commune,
            @Param("search") String search
    );
}