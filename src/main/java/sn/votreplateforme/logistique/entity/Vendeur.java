package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.votreplateforme.logistique.dto.StatutVendeur;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Vendeur - Hérite de User
 *
 * Représente un vendeur Instagram/Facebook qui crée des livraisons
 */
@Entity
@Table(name = "vendeurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendeur extends User {

    /**
     * Nom de la boutique (ex: "Fatou Fashion", "Modou Electronics")
     */
    @Column(length = 200)
    private String nomBoutique;

    /**
     * Catégorie d'activité (ex: "Vêtements", "Électronique", "Cosmétiques")
     */
    @Column(length = 100)
    private String categorieActivite;

    /**
     * Instagram handle (ex: "@fatoufashion")
     */
    @Column(length = 100)
    private String instagram;

    /**
     * Page Facebook
     */
    @Column(length = 200)
    private String facebook;

    // ==================== ADRESSE DE RAMASSAGE ====================

    /**
     * Commune du vendeur (pour le ramassage)
     */
    @Column(length = 100)
    private String commune;

    /**
     * Quartier du vendeur (pour le ramassage)
     */
    @Column(length = 100)
    private String quartier;

    /**
     * Adresse complète du vendeur (pour le ramassage)
     */
    @Column(columnDefinition = "TEXT")
    private String adresseComplete;

    // ==================== FINANCES ====================

    /**
     * Solde en attente de paiement
     * C'est l'argent que le vendeur a gagné mais pas encore reçu
     * Calculé : Somme des (montantCOD - fraisLivraison) des livraisons LIVREE non payées
     */
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal soldeEnAttente = BigDecimal.ZERO;

    // ==================== RELATIONS ====================

    /**
     * Liste des livraisons créées par ce vendeur
     * Relation OneToMany : Un vendeur a plusieurs livraisons
     */
    @OneToMany(mappedBy = "vendeur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Livraison> livraisons = new ArrayList<>();

    /**
     * Liste des transactions financières de ce vendeur
     * Relation OneToMany : Un vendeur a plusieurs transactions
     */
    @OneToMany(mappedBy = "vendeur", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatutVendeur statut = StatutVendeur.EN_ATTENTE_VALIDATION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Admin validePar;

    @Column(name = "valide_le")
    private LocalDateTime valideLe;

    @Column(name = "raison_suspension", columnDefinition = "TEXT")
    private String raisonSuspension;

    /**
     * Commission fixe négociée avec le partenaire, réglée par l'admin à la validation.
     * Elle sert de <b>prix de livraison fixe</b> pour chaque commande de ce vendeur
     * (remplace la tarification par zone). Null = pas encore fixée (repli tarif zone).
     */
    @Column(name = "commission_fixe", precision = 12, scale = 2)
    private BigDecimal commissionFixe;

    /**
     * Participation à la Dioks League (classement gamifié entre vendeurs).
     * Opt-in : tant que false, le vendeur n'apparaît pas dans le classement des autres.
     * Les stats étant calculées sur les livraisons réelles, quitter/revenir ne remet rien à zéro.
     */
    @Column(name = "participe_classement", nullable = false)
    @Builder.Default
    private boolean participeClassement = false;

    @Column(name = "date_adhesion_classement")
    private LocalDateTime dateAdhesionClassement;

    // Liaison Telegram : remontée sur User (commune à tous les rôles), cf. V17.

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Ajoute une livraison à la liste
     */
    public void ajouterLivraison(Livraison livraison) {
        livraisons.add(livraison);
        livraison.setVendeur(this);
    }

    /**
     * Retire une livraison de la liste
     */
    public void retirerLivraison(Livraison livraison) {
        livraisons.remove(livraison);
        livraison.setVendeur(null);
    }

    /**
     * Ajoute un montant au solde en attente
     */
    public void ajouterAuSolde(BigDecimal montant) {
        this.soldeEnAttente = this.soldeEnAttente.add(montant);
    }

    /**
     * Retire un montant du solde en attente (lors d'un paiement)
     */
    public void retirerDuSolde(BigDecimal montant) {
        this.soldeEnAttente = this.soldeEnAttente.subtract(montant);
    }
}
