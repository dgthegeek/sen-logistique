package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Quartier;
import sn.votreplateforme.logistique.entity.Zone;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Quartier
 */
@Repository
public interface QuartierRepository extends JpaRepository<Quartier, Long> {
    
    /**
     * Trouve tous les quartiers d'une commune
     * Utilisé pour l'auto-complétion dans le formulaire
     * 
     * @param commune Commune (ex: "Dakar", "Pikine")
     * @return Liste des quartiers de cette commune
     */
    List<Quartier> findByCommune(String commune);
    
    /**
     * Trouve tous les quartiers d'une commune qui sont actifs
     * 
     * @param commune Commune
     * @return Liste des quartiers actifs de cette commune
     */
    List<Quartier> findByCommuneAndActifTrue(String commune);
    
    /**
     * Trouve tous les quartiers d'une zone
     * 
     * @param zone Zone
     * @return Liste des quartiers de cette zone
     */
    List<Quartier> findByZone(Zone zone);
    
    /**
     * Trouve un quartier par son nom et sa commune
     * Car plusieurs communes peuvent avoir un quartier avec le même nom
     * 
     * @param nom Nom du quartier
     * @param commune Commune
     * @return Optional contenant le quartier si trouvé
     */
    Optional<Quartier> findByNomAndCommune(String nom, String commune);
    
    /**
     * Trouve tous les quartiers actifs
     * 
     * @return Liste des quartiers actifs
     */
    List<Quartier> findByActifTrue();
    
    /**
     * Trouve tous les quartiers, ordonnés par nom
     * Utile pour l'affichage alphabétique
     * 
     * @return Liste des quartiers triés par nom
     */
    List<Quartier> findAllByOrderByNomAsc();
}
