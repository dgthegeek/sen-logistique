package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.entity.Transaction;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.TransactionRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Service Finance - Gestion financière de la plateforme
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FinanceService {

    private final LivraisonRepository livraisonRepository;
    private final VendeurRepository vendeurRepository;
    private final TransactionRepository transactionRepository;
    private final FinanceCalculator financeCalculator;

    /**
     * Récupère le dashboard financier avec statistiques
     */
    @Transactional(readOnly = true)
    public AdminFinancesDashboard getDashboardFinancier(String periode) {
        log.info("Récupération dashboard financier - Période: {}", periode);

        // 1. Calculer les dates ou null si "tout"
        LocalDateTime dateDebut = "tout".equalsIgnoreCase(periode) ? null : calculerDateDebut(periode);
        LocalDateTime dateFin = LocalDateTime.now();

        // 2. Récupérer toutes les livraisons livrées selon la période
        List<Livraison> livraisonsLivrees;
        if ("tout".equalsIgnoreCase(periode)) {
            // Si période = "tout", récupérer TOUTES les livraisons livrées
            livraisonsLivrees = livraisonRepository.findByStatut(StatutLivraison.LIVREE);
        } else {
            // Sinon, filtrer par date
            livraisonsLivrees = livraisonRepository.findByStatutAndDateLivraisonBetween(
                    StatutLivraison.LIVREE,
                    dateDebut,
                    dateFin
            );
        }

        // 3. Calculer le cash collecté total
        BigDecimal cashCollecte = livraisonsLivrees.stream()
                .map(l -> l.getCashCollecte() != null ? l.getCashCollecte() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Calculer le montant à payer aux vendeurs (solde disponible dynamique)
        BigDecimal aPayerVendeurs = vendeurRepository.findAll().stream()
                .map(financeCalculator::soldeDisponible)
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Commission plateforme = l'intégralité des frais de livraison des commandes livrées.
        //    Pour chaque livraison, la plateforme gagne le prix de la livraison.
        BigDecimal commissions = livraisonsLivrees.stream()
                .map(l -> l.getFraisLivraison() != null ? l.getFraisLivraison() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Calculer les statistiques
        AdminFinancesDashboardStatistiques stats = new AdminFinancesDashboardStatistiques();
        stats.setNombreLivraisons(livraisonsLivrees.size());

        // Nombre de vendeurs distincts ayant eu des livraisons
        long nombreVendeurs = livraisonsLivrees.stream()
                .map(l -> l.getVendeur().getId())
                .distinct()
                .count();
        stats.setNombreVendeurs((int) nombreVendeurs);

        // Taux de réussite
        List<Livraison> toutesLivraisons;
        if ("tout".equalsIgnoreCase(periode)) {
            toutesLivraisons = livraisonRepository.findAll();
        } else {
            toutesLivraisons = livraisonRepository.findByDateCreationBetween(dateDebut, dateFin);
        }

        long livraisonsNonAnnulees = toutesLivraisons.stream()
                .filter(l -> l.getStatut() != StatutLivraison.ANNULEE)
                .count();

        double tauxReussite = livraisonsNonAnnulees > 0
                ? (livraisonsLivrees.size() * 100.0) / livraisonsNonAnnulees
                : 0.0;
        stats.setTauxReussite(tauxReussite);

        // 7. Construire la réponse
        AdminFinancesDashboard dashboard = new AdminFinancesDashboard();
        dashboard.setCashCollecte(cashCollecte);
        dashboard.setaPayerVendeurs(aPayerVendeurs);
        dashboard.setCommissions(commissions);
        dashboard.setStatistiques(stats);

        log.info("Dashboard financier généré - Période: {} - Cash: {} FCFA, À payer: {} FCFA",
                periode, cashCollecte, aPayerVendeurs);

        return dashboard;
    }
    /**
     * Récupère les demandes de paiement en attente
     */
    @Transactional(readOnly = true)
    public AdminFinancesPaiementsPendingGet200Response getDemandesPaiementEnAttente() {
        log.info("Récupération des demandes de paiement en attente");

        // 1. Parcourir tous les vendeurs et ne garder que ceux ayant un solde disponible > 0.
        //    Le solde est recalculé dynamiquement (CA livré - total déjà payé).
        List<Vendeur> tousLesVendeurs = vendeurRepository.findAll();

        // 2. Construire les demandes de paiement
        List<DemandePaiement> demandes = new ArrayList<>();
        BigDecimal totalAPayer = BigDecimal.ZERO;

        for (Vendeur vendeur : tousLesVendeurs) {
            BigDecimal soldeDisponible = financeCalculator.soldeDisponible(vendeur);
            if (soldeDisponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Compter le nombre de livraisons livrées
            long nombreLivraisons = livraisonRepository.countByVendeurAndStatut(
                    vendeur,
                    StatutLivraison.LIVREE
            );

            // Le vendeur a-t-il explicitement demandé un paiement ?
            var demandeEnAttente = transactionRepository
                    .findFirstByVendeurAndTypeAndStatutOrderByDateTransactionAsc(
                            vendeur,
                            Transaction.TypeTransaction.PAIEMENT_VENDEUR,
                            Transaction.StatutPaiement.EN_ATTENTE
                    );

            // Créer l'objet DemandePaiement
            DemandePaiement demande = new DemandePaiement();
            demande.setId(vendeur.getId());
            demande.setMontant(soldeDisponible);
            demande.setNombreLivraisons((int) nombreLivraisons);
            demande.setaDemande(demandeEnAttente.isPresent());

            // Date : celle de la demande explicite si elle existe, sinon la plus ancienne livraison
            if (demandeEnAttente.isPresent() && demandeEnAttente.get().getDateTransaction() != null) {
                demande.setDateDemande(
                        demandeEnAttente.get().getDateTransaction().atOffset(ZoneOffset.UTC)
                );
            } else {
                livraisonRepository.findFirstByVendeurAndStatutOrderByDateLivraisonAsc(
                        vendeur,
                        StatutLivraison.LIVREE
                ).ifPresent(livraison -> {
                    if (livraison.getDateLivraison() != null) {
                        demande.setDateDemande(
                                livraison.getDateLivraison().atOffset(ZoneOffset.UTC)
                        );
                    }
                });
            }

            // Vendeur (objet imbriqué)
            LivraisonDetailResponseVendeur vendeurInfo = new LivraisonDetailResponseVendeur();
            vendeurInfo.setId(vendeur.getId());
            vendeurInfo.setNom(vendeur.getNom());
            vendeurInfo.setPrenom(vendeur.getPrenom());
            vendeurInfo.setTelephone(vendeur.getTelephone());
            vendeurInfo.setNomBoutique(vendeur.getNomBoutique());
            demande.setVendeur(vendeurInfo);

            demandes.add(demande);
            totalAPayer = totalAPayer.add(soldeDisponible);
        }

        // Les demandes explicites du vendeur d'abord
        demandes.sort((a, b) -> Boolean.compare(
                Boolean.TRUE.equals(b.getaDemande()),
                Boolean.TRUE.equals(a.getaDemande())));

        // 3. Construire la réponse
        AdminFinancesPaiementsPendingGet200Response response =
                new AdminFinancesPaiementsPendingGet200Response();
        response.setDemandes(demandes);
        response.setTotalAPayer(totalAPayer);

        log.info("{} demandes de paiement - Total: {} FCFA", demandes.size(), totalAPayer);

        return response;
    }

    /**
     * Calcule la date de début selon la période
     */
    private LocalDateTime calculerDateDebut(String periode) {
        LocalDate today = LocalDate.now();

        return switch (periode) {
            case "jour" -> today.atStartOfDay();
            case "semaine" -> today.minusDays(7).atStartOfDay();
            case "mois" -> today.minusDays(30).atStartOfDay();
            default -> today.atStartOfDay();
        };
    }
}