package sn.votreplateforme.logistique.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Zone;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Zone
 */
@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByActiveTrue();

    Optional<Zone> findByNom(String nom);

    /**
     * Recherche avec filtres et pagination
     */
    @Query(value = "SELECT * FROM zones z " +
            "WHERE (:actif IS NULL OR z.active = :actif) " +
            "AND (:search IS NULL OR " +
            "     LOWER(z.nom) LIKE LOWER('%' || :search || '%') OR " +
            "     LOWER(z.description) LIKE LOWER('%' || :search || '%')) " +
            "ORDER BY z.nom ASC",
            nativeQuery = true)
    Page<Zone> searchZones(
            @Param("actif") Boolean actif,
            @Param("search") String search,
            Pageable pageable
    );
}