package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entité User - Classe abstraite parent pour Vendeur et Admin
 * 
 * Utilise l'héritage JPA avec stratégie JOINED :
 * - Table "users" contient les champs communs
 * - Table "vendeurs" contient les champs spécifiques aux vendeurs
 * - Table "admins" contient les champs spécifiques aux admins
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Nom de famille
     */
    @Column(nullable = false, length = 100)
    private String nom;
    
    /**
     * Prénom
     */
    @Column(nullable = false, length = 100)
    private String prenom;
    
    /**
     * Numéro de téléphone (unique)
     * Format: 77XXXXXXX, 78XXXXXXX, 76XXXXXXX, 70XXXXXXX, 75XXXXXXX
     */
    @Column(nullable = false, unique = true, length = 20)
    private String telephone;
    
    /**
     * Email (optionnel)
     */
    @Column(length = 150)
    private String email;
    
    /**
     * Mot de passe hashé (BCrypt)
     */
    @Column(nullable = false)
    private String password;
    
    /**
     * Rôle de l'utilisateur (VENDEUR ou ADMIN)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    
    /**
     * Compte actif ou non
     */
    @Column(nullable = false)
    private boolean actif = true;
    
    /**
     * Date d'inscription (auto-générée)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateInscription;

    /**
     * Identifiant de conversation Telegram (rempli après liaison du bot).
     * Disponible pour tous les rôles : chacun peut lier son compte dans "Mon compte"
     * et recevoir les notifications qui le concernent.
     */
    @Column(name = "telegram_chat_id", length = 50)
    private String telegramChatId;

    /**
     * Code temporaire de liaison Telegram (deep link t.me/bot?start=code).
     */
    @Column(name = "telegram_link_code", length = 40)
    private String telegramLinkCode;

    /**
     * Retourne le nom complet
     */
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
