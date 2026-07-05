package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.User;

import java.util.Optional;

/**
 * Repository pour l'entité User
 * 
 * Hérite de JpaRepository qui fournit automatiquement :
 * - save(user)
 * - findById(id)
 * - findAll()
 * - delete(user)
 * - count()
 * - etc.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Trouve un utilisateur par son numéro de téléphone
     * Utilisé pour la connexion
     * 
     * @param telephone Numéro de téléphone (ex: "771234567")
     * @return Optional contenant l'utilisateur si trouvé
     */
    Optional<User> findByTelephone(String telephone);
    
    /**
     * Vérifie si un téléphone existe déjà
     * Utilisé lors de l'inscription pour éviter les doublons
     * 
     * @param telephone Numéro de téléphone
     * @return true si le téléphone existe
     */
    boolean existsByTelephone(String telephone);
    
    /**
     * Trouve un utilisateur par son email
     * 
     * @param email Email de l'utilisateur
     * @return Optional contenant l'utilisateur si trouvé
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Vérifie si un email existe déjà
     * 
     * @param email Email
     * @return true si l'email existe
     */
    boolean existsByEmail(String email);

    /**
     * Trouve un utilisateur par son code temporaire de liaison Telegram.
     * Utilisé lors du polling getUpdates pour associer un chat à un compte.
     */
    Optional<User> findByTelegramLinkCode(String telegramLinkCode);
}
