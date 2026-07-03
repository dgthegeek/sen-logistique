package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Livraison - Cœur du système
 *
 * Représente une livraison créée par un vendeur
 * Cycle de vie : EN_ATTENTE_RAMASSAGE → RAMASSE → EN_ROUTE → LIVREE
 */
@Entity
@Table(name = "livraisons")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== TRACKING ====================

    /**
     * Numéro de tracking unique (ex: "DKR-00567")
     * Généré automatiquement à la création
     */
    @Column(nullable = false, unique = true, length = 50)
    private String numeroTracking;

    /**
     * URL du QR code pour la confirmation de livraison
     * Ex: "https://track.votreplateforme.sn/DKR-00567/deliver"
     */
    @Column(length = 500)
    private String qrCodeUrl;

    // ==================== VENDEUR ====================

    /**
     * Vendeur qui a créé cette livraison
     * Relation ManyToOne : Plusieurs livraisons appartiennent à un vendeur
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Vendeur vendeur;

    // ==================== CLIENT FINAL ====================

    /**
     * Nom complet du client final (celui qui reçoit le colis)
     */
    @Column(nullable = false, length = 200)
    private String nomClient;

    /**
     * Téléphone du client final
     * Format: 77XXXXXXX, 78XXXXXXX, etc.
     */
    @Column(nullable = false, length = 20)
    private String telephoneClient;

    /**
     * Adresse de destination (embeddable)
     * Contient : commune, quartier, adresseComplete, pointRepere, zone
     */
    @Embedded
    private Adresse adresseDestination;

    // ==================== DÉTAILS DU COLIS ====================

    /**
     * Description du produit (ex: "Ensemble wax", "iPhone 13")
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descriptionProduit;

    /**
     * Colis fragile ou non
     */
    @Column(nullable = false)
    private Boolean fragile = false;

    /**
     * Poids du colis en kg (optionnel)
     * Utilisé pour calculer un supplément si > 5kg ou > 10kg
     */
    @Column(name = "poids", precision = 10, scale = 2)
    private BigDecimal poids;

    // ==================== FINANCES ====================

    /**
     * Montant Cash on Delivery (COD)
     * C'est ce que le client final doit payer
     */
    @Column(name = "montant_cod", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantCOD;

    /**
     * Frais de livraison
     * Calculé automatiquement selon la zone et l'urgence
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fraisLivraison;

    /**
     * Cash collecté lors de la livraison
     * Doit normalement être égal à montantCOD
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal cashCollecte;

    // ==================== STATUT & DATES ====================

    /**
     * Statut actuel de la livraison
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutLivraison statut = StatutLivraison.NOUVELLE;

    /**
     * Date de création de la livraison (auto-générée)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Date de ramassage (quand admin marque "RAMASSE")
     */
    private LocalDateTime dateRamassage;

    /**
     * Date de mise en route (quand admin part livrer)
     */
    private LocalDateTime dateEnRoute;

    /**
     * Date de livraison finale (quand admin confirme "LIVREE")
     */
    private LocalDateTime dateLivraison;

    // ==================== OPTIONS ====================

    /**
     * Type d'urgence (NORMAL ou EXPRESS)
     * Impact sur le tarif
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeUrgence urgence = TypeUrgence.NORMAL;

    /**
     * Créneau souhaité (MATIN, APRES_MIDI, SOIR)
     */
    @Column(length = 20)
    private String creneauSouhaite;

    /**
     * Notes pour le livreur (ex: "Appeler avant d'arriver")
     */
    @Column(length = 500)
    private String notesPourLivreur;

    // ==================== CLOSING & DISPATCH ====================

    /**
     * Livreur assigné à cette livraison (module Dispatch).
     * Null tant que la commande n'a pas été assignée par l'admin.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livreur_id")
    private Livreur livreur;

    /**
     * Closeur qui a pris la commande en charge (traçabilité qualité).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closeur_id")
    private Closeur closeur;

    /**
     * Dispatcheur qui a assigné la commande à un livreur (traçabilité qualité).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatcheur_id")
    private Dispatcheur dispatcheur;

    /**
     * Date de prise en charge par le closeur (première action : appel/confirmation).
     */
    private LocalDateTime datePriseEnCharge;

    /**
     * Date de confirmation par le closeur (passage à CONFIRMEE).
     */
    private LocalDateTime dateConfirmation;

    /**
     * Date à laquelle la commande est devenue "prête à livrer" (fin du closing).
     */
    private LocalDateTime datePreteALivrer;

    /**
     * Date d'assignation à un livreur (passage à ASSIGNEE).
     */
    private LocalDateTime dateAssignation;

    /**
     * Motif d'échec, obligatoire lorsque le statut passe à ECHEC.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "motif_echec", length = 30)
    private MotifEchec motifEchec;

    /**
     * Date à laquelle la livraison a échoué (passage à ECHEC).
     */
    private LocalDateTime dateEchec;

    // ==================== STOCK ====================

    /**
     * Produit lié à la commande (optionnel). Si présent, le stock est
     * automatiquement décrémenté à la livraison.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    /**
     * Quantité commandée du produit (utilisée pour le décrément de stock).
     * Conservé pour compatibilité ; le multi-produits passe par {@link #lignes}.
     */
    @Column(name = "quantite")
    private Integer quantite;

    /**
     * Lignes de commande (multi-produits). Si non vide, prime sur produit/quantite.
     */
    @OneToMany(mappedBy = "livraison", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<LigneCommande> lignes = new java.util.ArrayList<>();

    public void ajouterLigne(LigneCommande ligne) {
        ligne.setLivraison(this);
        this.lignes.add(ligne);
    }

    // ==================== APRÈS LIVRAISON ====================

    /**
     * Commentaire après livraison (ex: "Livraison OK, cliente satisfaite")
     */
    @Column(columnDefinition = "TEXT")
    private String commentaireLivraison;

    /**
     * Latitude GPS de la livraison (optionnel pour V1)
     */
    private BigDecimal latitudeLivraison;

    /**
     * Longitude GPS de la livraison (optionnel pour V1)
     */
    private BigDecimal longitudeLivraison;

    // ==================== MÉTHODES UTILITAIRES ====================
    public void setVendeur(Vendeur vendeur) {
        this.vendeur = vendeur;
    }

    /**
     * Calcule le montant que le vendeur va recevoir
     * montantCOD - fraisLivraison
     */
    public BigDecimal getMontantVendeur() {
        return montantCOD.subtract(fraisLivraison);
    }

    /**
     * Vérifie si la livraison est terminée (livrée ou échouée)
     */
    public boolean estTerminee() {
        return statut == StatutLivraison.LIVREE
            || statut == StatutLivraison.ECHEC
            || statut == StatutLivraison.ECHEC_ABSENT
            || statut == StatutLivraison.ECHEC_REFUSE
            || statut == StatutLivraison.ANNULEE;
    }

    /**
     * Vérifie si la livraison a réussi
     */
    public boolean estReussie() {
        return statut == StatutLivraison.LIVREE;
    }

    /**
     * Marque la livraison comme ramassée
     */
    public void marquerRamasse() {
        this.statut = StatutLivraison.RAMASSE;
        this.dateRamassage = LocalDateTime.now();
    }

    /**
     * Marque la livraison en route
     */
    public void marquerEnRoute() {
        this.statut = StatutLivraison.EN_ROUTE;
        this.dateEnRoute = LocalDateTime.now();
    }

    /**
     * Marque la livraison comme livrée
     */
    public void marquerLivree(BigDecimal cashCollecte, String commentaire) {
        this.statut = StatutLivraison.LIVREE;
        this.dateLivraison = LocalDateTime.now();
        this.cashCollecte = cashCollecte;
        this.commentaireLivraison = commentaire;
    }

    // ==================== TRANSITIONS CLOSING & DISPATCH ====================

    /**
     * Le closeur prend la commande en charge pour appel (NOUVELLE -> A_APPELER).
     */
    public void marquerAAppeler() {
        this.statut = StatutLivraison.A_APPELER;
        if (this.datePriseEnCharge == null) {
            this.datePriseEnCharge = LocalDateTime.now();
        }
    }

    /**
     * Le closeur confirme la commande après accord du client (-> CONFIRMEE).
     */
    public void marquerConfirmee() {
        this.statut = StatutLivraison.CONFIRMEE;
        this.dateConfirmation = LocalDateTime.now();
        if (this.datePriseEnCharge == null) {
            this.datePriseEnCharge = LocalDateTime.now();
        }
    }

    /**
     * La commande confirmée devient disponible pour le dispatch (-> PRETE_A_LIVRER).
     */
    public void marquerPreteALivrer() {
        this.statut = StatutLivraison.PRETE_A_LIVRER;
        this.datePreteALivrer = LocalDateTime.now();
    }

    /**
     * L'admin assigne la commande à un livreur (-> ASSIGNEE).
     */
    public void assignerLivreur(Livreur livreur) {
        this.livreur = livreur;
        this.statut = StatutLivraison.ASSIGNEE;
        this.dateAssignation = LocalDateTime.now();
    }

    /**
     * Le livreur démarre la livraison (-> EN_LIVRAISON).
     */
    public void marquerEnLivraison() {
        this.statut = StatutLivraison.EN_LIVRAISON;
        this.dateEnRoute = LocalDateTime.now();
    }

    /**
     * Échec de livraison avec motif obligatoire (-> ECHEC).
     */
    public void marquerEchec(MotifEchec motif, String commentaire) {
        this.statut = StatutLivraison.ECHEC;
        this.motifEchec = motif;
        this.commentaireLivraison = commentaire;
        this.dateEchec = LocalDateTime.now();
    }

    /**
     * Annule la commande (-> ANNULEE).
     */
    public void annuler(String commentaire) {
        this.statut = StatutLivraison.ANNULEE;
        this.commentaireLivraison = commentaire;
    }
}
