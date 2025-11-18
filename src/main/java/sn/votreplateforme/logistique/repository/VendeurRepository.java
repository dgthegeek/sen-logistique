package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Vendeur;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Vendeur
 */
@Repository
public interface VendeurRepository extends JpaRepository<Vendeur, Long> {
    
    /**
     * Trouve un vendeur par son numéro de téléphone
     * Utilisé pour l'authentification
     * 
     * @param telephone Numéro de téléphone
     * @return Optional contenant le vendeur si trouvé
     */
    Optional<Vendeur> findByTelephone(String telephone);
    
    /**
     * Trouve un vendeur par son email
     * 
     * @param email Email
     * @return Optional contenant le vendeur si trouvé
     */
    Optional<Vendeur> findByEmail(String email);
    
    /**
     * Trouve tous les vendeurs actifs
     * 
     * @return Liste des vendeurs actifs
     */
    List<Vendeur> findByActifTrue();
    
    /**
     * Trouve les vendeurs par commune (pour les ramassages groupés)
     * 
     * @param commune Commune (ex: "Dakar", "Pikine")
     * @return Liste des vendeurs dans cette commune
     */
    List<Vendeur> findByCommune(String commune);
    
    /**
     * Trouve les vendeurs par quartier (pour les ramassages groupés)
     * 
     * @param quartier Quartier (ex: "Mermoz", "Sacré-Coeur")
     * @return Liste des vendeurs dans ce quartier
     */
    List<Vendeur> findByQuartier(String quartier);
    
    /**
     * Trouve les vendeurs qui ont un solde en attente supérieur à un montant
     * Utilisé pour identifier les vendeurs à payer
     * 
     * @param montant Montant minimum
     * @return Liste des vendeurs avec solde >= montant
     */
    List<Vendeur> findBySoldeEnAttenteGreaterThanEqual(BigDecimal montant);
    
    /**
     * Compte le nombre de vendeurs actifs
     * 
     * @return Nombre de vendeurs actifs
     */
    long countByActifTrue();
    
    /**
     * Calcule le solde total en attente de tous les vendeurs
     * Utilisé pour le dashboard admin
     * 
     * @return Somme de tous les soldes en attente
     */
    @Query("SELECT COALESCE(SUM(v.soldeEnAttente), 0) FROM Vendeur v WHERE v.actif = true")
    BigDecimal calculerSoldeTotalEnAttente();
}
