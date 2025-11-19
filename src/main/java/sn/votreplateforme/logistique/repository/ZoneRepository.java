package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Zone;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Zone
 */
@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {
    
    /**
     * Trouve une zone par son nom
     * 
     * @param nom Nom de la zone (ex: "Zone 1", "Zone 2")
     * @return Optional contenant la zone si trouvée
     */
    Optional<Zone> findByNom(String nom);
    
    /**
     * Trouve toutes les zones actives
     * Utilisé pour l'affichage dans le formulaire de création de livraison
     * 
     * @return Liste des zones actives
     */
    List<Zone> findByActiveTrue();
    
    /**
     * Trouve toutes les zones, ordonnées par tarif standard croissant
     * Utile pour afficher les zones du moins cher au plus cher
     * 
     * @return Liste des zones triées par tarif
     */
    List<Zone> findAllByOrderByTarifStandardAsc();
    
    /**
     * Compte le nombre de zones actives
     * 
     * @return Nombre de zones actives
     */
    long countByActiveTrue();

}
