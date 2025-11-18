package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Admin;

import java.util.Optional;

/**
 * Repository pour l'entité Admin
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    /**
     * Trouve un admin par son numéro de téléphone
     * Utilisé pour l'authentification
     * 
     * @param telephone Numéro de téléphone
     * @return Optional contenant l'admin si trouvé
     */
    Optional<Admin> findByTelephone(String telephone);
    
    /**
     * Trouve un admin par son email
     * 
     * @param email Email
     * @return Optional contenant l'admin si trouvé
     */
    Optional<Admin> findByEmail(String email);
}
