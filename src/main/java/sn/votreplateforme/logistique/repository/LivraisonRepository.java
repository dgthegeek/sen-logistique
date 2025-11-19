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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Livraison
 * Le plus important du système !
 */
@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long> {
    
    // ==================== RECHERCHE PAR NUMÉRO ====================
    
    /**
     * Trouve une livraison par son numéro de tracking
     * ⭐ TRÈS UTILISÉ - Pour le tracking public et la confirmation
     * 
     * @param numeroTracking Numéro de tracking (ex: "DKR-00567")
     * @return Optional contenant la livraison si trouvée
     */
    Optional<Livraison> findByNumeroTracking(String numeroTracking);
    
    /**
     * Vérifie si un numéro de tracking existe déjà
     * Utilisé lors de la génération pour éviter les doublons
     * 
     * @param numeroTracking Numéro de tracking
     * @return true si le numéro existe
     */
    boolean existsByNumeroTracking(String numeroTracking);
    
    // ==================== RECHERCHE PAR VENDEUR ====================

    /**
     * Trouve toutes les livraisons d'un vendeur
     *
     * @param vendeur Vendeur
     * @return Liste des livraisons du vendeur
     */
    List<Livraison> findByVendeur(Vendeur vendeur);
    
    /**
     * Trouve les livraisons d'un vendeur avec pagination
     * Utilisé dans le dashboard vendeur
     * 
     * @param vendeur Vendeur
     * @param pageable Pagination
     * @return Page de livraisons
     */
    Page<Livraison> findByVendeur(Vendeur vendeur, Pageable pageable);
    
    /**
     * Trouve les livraisons d'un vendeur avec un statut spécifique
     * 
     * @param vendeur Vendeur
     * @param statut Statut
     * @return Liste des livraisons
     */
    List<Livraison> findByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut);
    
    /**
     * Compte les livraisons d'un vendeur par statut
     * Utilisé pour les statistiques du dashboard vendeur
     * 
     * @param vendeur Vendeur
     * @param statut Statut
     * @return Nombre de livraisons
     */
    long countByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut);
    
    // ==================== RECHERCHE PAR STATUT ====================
    
    /**
     * Trouve toutes les livraisons avec un statut donné
     * 
     * @param statut Statut
     * @return Liste des livraisons
     */
    List<Livraison> findByStatut(StatutLivraison statut);
    
    /**
     * Trouve les livraisons par statut avec pagination
     * Utilisé dans le dashboard admin
     * 
     * @param statut Statut
     * @param pageable Pagination
     * @return Page de livraisons
     */
    Page<Livraison> findByStatut(StatutLivraison statut, Pageable pageable);
    
    /**
     * Compte les livraisons par statut
     * 
     * @param statut Statut
     * @return Nombre de livraisons
     */
    long countByStatut(StatutLivraison statut);
    
    // ==================== RECHERCHE PAR DATE ====================
    
    /**
     * Trouve les livraisons créées après une date
     * 
     * @param date Date de début
     * @return Liste des livraisons
     */
    List<Livraison> findByDateCreationAfter(LocalDateTime date);
    
    /**
     * Trouve les livraisons créées entre deux dates
     * Utilisé pour les statistiques et rapports
     * 
     * @param debut Date de début
     * @param fin Date de fin
     * @return Liste des livraisons
     */
    List<Livraison> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);
    
    /**
     * Trouve les livraisons livrées aujourd'hui
     * Utilisé pour le rapport journalier
     * 
     * @param debut Début du jour (00:00:00)
     * @param fin Fin du jour (23:59:59)
     * @return Liste des livraisons livrées aujourd'hui
     */
    @Query("SELECT l FROM Livraison l WHERE l.statut = 'LIVREE' AND l.dateLivraison BETWEEN :debut AND :fin")
    List<Livraison> findLivraisonsDuJour(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
    
    // ==================== RAMASSAGES ====================
    
    /**
     * Trouve toutes les livraisons en attente de ramassage
     * ⭐ IMPORTANT - Utilisé pour le dashboard admin ramassages
     * 
     * @return Liste des livraisons à ramasser
     */
    List<Livraison> findByStatutOrderByDateCreationAsc(StatutLivraison statut);
    
    /**
     * Trouve les livraisons en attente de ramassage pour un quartier
     * Utilisé pour les ramassages groupés par zone
     * 
     * @param statut Statut
     * @param quartier Quartier
     * @return Liste des livraisons
     */
    @Query("SELECT l FROM Livraison l WHERE l.statut = :statut AND l.vendeur.quartier = :quartier ORDER BY l.dateCreation ASC")
    List<Livraison> findByStatutAndVendeurQuartier(@Param("statut") StatutLivraison statut, @Param("quartier") String quartier);
    
    // ==================== LIVRAISONS À EFFECTUER ====================
    
    /**
     * Trouve les livraisons ramassées (prêtes pour livraison)
     * ⭐ IMPORTANT - Utilisé pour le dashboard admin livraisons
     * 
     * @return Liste des livraisons à livrer
     */
    List<Livraison> findByStatutInOrderByDateRamassageAsc(List<StatutLivraison> statuts);
    
    /**
     * Trouve les livraisons à livrer dans un quartier
     * Utilisé pour optimiser les tournées de livraison
     * 
     * @param statuts Liste des statuts (RAMASSE, EN_ROUTE)
     * @param quartier Quartier de destination
     * @return Liste des livraisons
     */
    @Query("SELECT l FROM Livraison l WHERE l.statut IN :statuts AND l.adresseDestination.quartier = :quartier ORDER BY l.dateRamassage ASC")
    List<Livraison> findByStatutsAndQuartierDestination(@Param("statuts") List<StatutLivraison> statuts, @Param("quartier") String quartier);
    
    // ==================== STATISTIQUES ====================
    
    /**
     * Calcule le total du cash collecté pour les livraisons livrées
     * Utilisé pour le dashboard admin finances
     * 
     * @return Somme du cash collecté
     */
    @Query("SELECT COALESCE(SUM(l.cashCollecte), 0) FROM Livraison l WHERE l.statut = 'LIVREE'")
    BigDecimal calculerCashTotalCollecte();
    
    /**
     * Calcule le total des frais de livraison pour les livraisons livrées
     * 
     * @return Somme des frais de livraison
     */
    @Query("SELECT COALESCE(SUM(l.fraisLivraison), 0) FROM Livraison l WHERE l.statut = 'LIVREE'")
    BigDecimal calculerFraisTotalLivraison();
    
    /**
     * Calcule les statistiques d'un vendeur
     * 
     * @param vendeur Vendeur
     * @param debut Date de début
     * @param fin Date de fin
     * @return Nombre de livraisons dans la période
     */
    long countByVendeurAndDateCreationBetween(Vendeur vendeur, LocalDateTime debut, LocalDateTime fin);
    
    /**
     * Calcule le CA généré par un vendeur (somme des montantCOD)
     * 
     * @param vendeur Vendeur
     * @param debut Date de début
     * @param fin Date de fin
     * @return CA généré
     */
    @Query("SELECT COALESCE(SUM(l.montantCOD), 0) FROM Livraison l WHERE l.vendeur = :vendeur AND l.statut = 'LIVREE' AND l.dateLivraison BETWEEN :debut AND :fin")
    BigDecimal calculerCAVendeur(@Param("vendeur") Vendeur vendeur, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
    
    // ==================== RECHERCHE AVANCÉE ====================
    
    /**
     * Recherche de livraisons par plusieurs critères
     * Utilisé pour le filtrage dans le dashboard admin
     * 
     * @param vendeur Vendeur (optionnel)
     * @param statut Statut (optionnel)
     * @param debut Date de début (optionnel)
     * @param fin Date de fin (optionnel)
     * @param pageable Pagination
     * @return Page de livraisons
     */
    @Query("SELECT l FROM Livraison l WHERE " +
           "(:vendeur IS NULL OR l.vendeur = :vendeur) AND " +
           "(:statut IS NULL OR l.statut = :statut) AND " +
           "(:debut IS NULL OR l.dateCreation >= :debut) AND " +
           "(:fin IS NULL OR l.dateCreation <= :fin) " +
           "ORDER BY l.dateCreation DESC")
    Page<Livraison> rechercherLivraisons(
        @Param("vendeur") Vendeur vendeur,
        @Param("statut") StatutLivraison statut,
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin,
        Pageable pageable
    );

    /**
     * Compte le nombre de livraisons dont le numéro de tracking commence par le préfixe donné
     */
    long countByNumeroTrackingStartingWith(String prefix);

    /**
     * Trouve les livraisons d'un vendeur avec un statut et pagination
     */
    Page<Livraison> findByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut, Pageable pageable);
}
