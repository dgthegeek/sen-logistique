package sn.votreplateforme.logistique.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.entity.Zone;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Livraison
 */
@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long> {

    // ==================== RECHERCHES PAR NUMERO TRACKING ====================

    List<Livraison> findByVendeurId(Long vendeurId);


    Optional<Livraison> findByNumeroTracking(String numeroTracking);

    boolean existsByNumeroTracking(String numeroTracking);

    long countByNumeroTrackingStartingWith(String prefix);

    long countByAdresseDestination_Quartier(String quartier);

    // ==================== RECHERCHES PAR VENDEUR ====================

    List<Livraison> findByVendeur(Vendeur vendeur);

    Page<Livraison> findByVendeur(Vendeur vendeur, Pageable pageable);

    long countByVendeur(Vendeur vendeur);

    // ==================== RECHERCHES PAR VENDEUR ET STATUT ====================

    List<Livraison> findByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut);

    Page<Livraison> findByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut, Pageable pageable);

    long countByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut);

    // ==================== RECHERCHES PAR STATUT ====================

    List<Livraison> findByStatut(StatutLivraison statut);

    Page<Livraison> findByStatut(StatutLivraison statut, Pageable pageable);

    long countByStatut(StatutLivraison statut);

    List<Livraison> findByStatutOrderByDateCreationAsc(StatutLivraison statut);

    List<Livraison> findByStatutInOrderByDateRamassageAsc(List<StatutLivraison> statuts);

    // ==================== CLOSING & DISPATCH ====================

    /** File du closeur : commandes à traiter, plus anciennes d'abord. */
    List<Livraison> findByStatutInOrderByDateCreationAsc(List<StatutLivraison> statuts);

    /** "Mes livraisons" d'un livreur, filtrées par statuts. */
    List<Livraison> findByLivreur_IdAndStatutInOrderByDateAssignationDesc(
            Long livreurId, List<StatutLivraison> statuts);

    /** Toutes les livraisons d'un livreur. */
    List<Livraison> findByLivreur_IdOrderByDateAssignationDesc(Long livreurId);

    /** Nombre de livraisons en cours pour un livreur. */
    long countByLivreur_IdAndStatutIn(Long livreurId, List<StatutLivraison> statuts);

    // ==================== STATS / DASHBOARD ====================

    long countByLivreur_IdAndStatut(Long livreurId, StatutLivraison statut);

    List<Livraison> findByLivreur_IdAndStatut(Long livreurId, StatutLivraison statut);

    /**
     * Agrégat par zone : [nomZone, nbLivraisons, chiffreAffaires (LIVREE), nbEchecs].
     * Les échecs couvrent le nouveau statut ECHEC et les anciens (dormant).
     */
    @Query("SELECT z.nom, COUNT(l), " +
            "COALESCE(SUM(CASE WHEN l.statut = 'LIVREE' THEN l.montantCOD ELSE 0 END), 0), " +
            "SUM(CASE WHEN l.statut IN ('ECHEC','ECHEC_ABSENT','ECHEC_REFUSE') THEN 1 ELSE 0 END) " +
            "FROM Livraison l JOIN l.adresseDestination.zone z " +
            "GROUP BY z.nom ORDER BY COUNT(l) DESC")
    List<Object[]> statsParZone();

    // ==================== RECHERCHES PAR DATES ====================

    List<Livraison> findByDateCreationAfter(LocalDateTime date);

    List<Livraison> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    List<Livraison> findByVendeurAndDateCreationBetween(
            Vendeur vendeur,
            LocalDateTime dateDebut,
            LocalDateTime dateFin
    );

    long countByVendeurAndDateCreationBetween(
            Vendeur vendeur,
            LocalDateTime debut,
            LocalDateTime fin
    );

    List<Livraison> findByStatutAndDateLivraisonBetween(
            StatutLivraison statut,
            LocalDateTime dateDebut,
            LocalDateTime dateFin
    );

    /** Nombre d'échecs survenus dans une plage (par date d'échec). */
    long countByStatutAndDateEchecBetween(
            StatutLivraison statut,
            LocalDateTime debut,
            LocalDateTime fin
    );

    @Query("SELECT l FROM Livraison l WHERE l.statut = 'LIVREE' AND l.dateLivraison BETWEEN :debut AND :fin")
    List<Livraison> findLivraisonsDuJour(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // ==================== RECHERCHES PAR ZONE ====================

    List<Livraison> findByAdresseDestinationZoneAndStatut(Zone zone, StatutLivraison statut);

    @Query("SELECT l FROM Livraison l WHERE l.statut = :statut AND l.vendeur.quartier = :quartier ORDER BY l.dateCreation ASC")
    List<Livraison> findByStatutAndVendeurQuartier(@Param("statut") StatutLivraison statut, @Param("quartier") String quartier);

    @Query("SELECT l FROM Livraison l WHERE l.statut IN :statuts AND l.adresseDestination.quartier = :quartier ORDER BY l.dateRamassage ASC")
    List<Livraison> findByStatutsAndQuartierDestination(@Param("statuts") List<StatutLivraison> statuts, @Param("quartier") String quartier);

    // ==================== CALCULS FINANCIERS ====================

    @Query("SELECT COALESCE(SUM(l.cashCollecte), 0) FROM Livraison l WHERE l.statut = 'LIVREE'")
    BigDecimal calculerCashTotalCollecte();

    @Query("SELECT COALESCE(SUM(l.fraisLivraison), 0) FROM Livraison l WHERE l.statut = 'LIVREE'")
    BigDecimal calculerFraisTotalLivraison();

    @Query("SELECT COALESCE(SUM(l.montantCOD), 0) FROM Livraison l WHERE l.vendeur = :vendeur AND l.statut = 'LIVREE' AND l.dateLivraison BETWEEN :debut AND :fin")
    BigDecimal calculerCAVendeur(@Param("vendeur") Vendeur vendeur, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Chiffre d'affaires "produit" cumulé d'un vendeur : somme du prix produit
     * (COD hors frais de livraison) de toutes ses livraisons livrées, sans borne de date.
     * C'est la source unique du CA et de la base du solde disponible.
     */
    @Query("SELECT COALESCE(SUM(l.montantCOD - l.fraisLivraison), 0) FROM Livraison l WHERE l.vendeur = :vendeur AND l.statut = 'LIVREE'")
    BigDecimal sumProduitLivrees(@Param("vendeur") Vendeur vendeur);

    /**
     * Statistiques de livraisons livrées agrégées par vendeur (Dioks League).
     * Retourne [vendeurId (Long), nombreLivraisons (Long), chiffreAffaires (BigDecimal)].
     */
    @Query("SELECT l.vendeur.id, COUNT(l), COALESCE(SUM(l.montantCOD - l.fraisLivraison), 0) " +
            "FROM Livraison l WHERE l.statut = 'LIVREE' GROUP BY l.vendeur.id")
    List<Object[]> statsLivreesParVendeur();

    // ==================== RECHERCHES AVANCÉES ====================

    /**
     * Recherche avancée avec filtres optionnels
     * CORRECTION: Utilisation de COALESCE pour gérer les paramètres NULL
     */
    @Query("SELECT l FROM Livraison l WHERE " +
            "(:vendeur IS NULL OR l.vendeur = :vendeur) AND " +
            "(:statut IS NULL OR l.statut = :statut) AND " +
            "(CAST(:debut AS timestamp) IS NULL OR l.dateCreation >= :debut) AND " +
            "(CAST(:fin AS timestamp) IS NULL OR l.dateCreation <= :fin) " +
            "ORDER BY l.dateCreation DESC")
    Page<Livraison> rechercherLivraisons(
            @Param("vendeur") Vendeur vendeur,
            @Param("statut") StatutLivraison statut,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );

    Optional<Livraison> findFirstByVendeurAndStatutOrderByDateLivraisonAsc(
            Vendeur vendeur,
            StatutLivraison statut
    );

    long countByAdresseDestination_ZoneId(Long zoneId);


}