package sn.votreplateforme.logistique.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /** Résout le vendeur propriétaire d'une clé API d'intégration (Shopify, script...). */
    Optional<Vendeur> findByApiKey(String apiKey);

    /**
     * Verrouille la ligne du vendeur pour la durée de la transaction courante
     * (SELECT ... FOR UPDATE). Empêche deux paiements concurrents pour le même
     * vendeur de lire le même solde disponible avant qu'aucun n'ait validé, ce
     * qui aurait pu le payer deux fois. Un second appel concurrent attend que
     * le premier ait validé, puis relit un solde à jour (déjà remis à zéro).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vendeur v WHERE v.id = :id")
    Optional<Vendeur> findByIdForUpdate(@Param("id") Long id);

    List<Vendeur> findBySoldeEnAttenteGreaterThan(BigDecimal montant);

    List<Vendeur> findByStatut(StatutVendeur statut);

    /** Vendeurs participant à la Dioks League (classement). */
    List<Vendeur> findByParticipeClassementTrue();

    /** Total dû aux partenaires (somme des soldes en attente). */
    @Query("SELECT COALESCE(SUM(v.soldeEnAttente), 0) FROM Vendeur v")
    BigDecimal sumSoldeEnAttente();

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