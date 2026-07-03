package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.*;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.TransactionRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour les fonctionnalités vendeur (dashboard, finances, etc.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VendeurService {

    private final VendeurRepository vendeurRepository;
    private final LivraisonRepository livraisonRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final FinanceCalculator financeCalculator;

    /**
     * Récupère le dashboard du vendeur avec statistiques du jour
     */
    @Transactional(readOnly = true)
    public VendeurDashboard getDashboard() {
        log.info("=== Récupération dashboard vendeur ===");

        Vendeur vendeur = getCurrentVendeur();

        // Statistiques du jour (aujourd'hui)
//        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
//        LocalDateTime finJour = debutJour.plusDays(1);

        //Note on va changer et utiliser les statistique des 30 derniers jours
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = fin.minusDays(30);


        List<Livraison> livraisonsJour = livraisonRepository
                .findByVendeurAndDateCreationBetween(vendeur, debut, fin);

        int totalColis = livraisonsJour.size();
        int colisLivres = (int) livraisonsJour.stream()
                .filter(l -> l.getStatut() == StatutLivraison.LIVREE)
                .count();
        // Échecs / annulations : ne doivent PAS être comptés comme "en cours"
        int colisTermines = (int) livraisonsJour.stream()
                .filter(l -> l.getStatut() == StatutLivraison.ECHEC
                        || l.getStatut() == StatutLivraison.ECHEC_ABSENT
                        || l.getStatut() == StatutLivraison.ECHEC_REFUSE
                        || l.getStatut() == StatutLivraison.ANNULEE)
                .count();
        int colisEnCours = Math.max(0, totalColis - colisLivres - colisTermines);

        // Solde disponible + chiffre d'affaires cumulé (source unique : FinanceCalculator)
        BigDecimal soldeEnAttente = financeCalculator.soldeDisponible(vendeur);
        BigDecimal chiffreAffaires = financeCalculator.chiffreAffaires(vendeur);

        // Construire les statistiques du jour
        VendeurDashboardStatistiquesJour statsJour = new VendeurDashboardStatistiquesJour();
        statsJour.setTotalColis(totalColis);
        statsJour.setColisLivres(colisLivres);
        statsJour.setColisEnCours(colisEnCours);

        // Construire les finances
        VendeurDashboardFinances finances = new VendeurDashboardFinances();
        finances.setSoldeEnAttente(soldeEnAttente);
        finances.setChiffreAffaires(chiffreAffaires);
        finances.setProchainPaiement("Sur demande");

        // Construire le dashboard
        VendeurDashboard dashboard = new VendeurDashboard();
        dashboard.setStatistiquesJour(statsJour);
        dashboard.setFinances(finances);

        log.info("✅ Dashboard généré - Colis jour: {}, Livrés: {}, Solde: {} FCFA",
                totalColis, colisLivres, soldeEnAttente);

        return dashboard;
    }

    /**
     * Récupère l'état financier détaillé du vendeur
     */
    @Transactional(readOnly = true)
    public VendeurFinances getFinances() {
        log.info("=== Récupération finances vendeur ===");

        Vendeur vendeur = getCurrentVendeur();

        // Solde disponible + chiffre d'affaires cumulé (source unique : FinanceCalculator)
        BigDecimal soldeEnAttente = financeCalculator.soldeDisponible(vendeur);
        BigDecimal chiffreAffaires = financeCalculator.chiffreAffaires(vendeur);

        // Statistiques du mois en cours : uniquement les livraisons LIVREE du mois
        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMois = debutMois.plusMonths(1);

        List<Livraison> livraisonsMois = livraisonRepository
                .findByVendeurAndDateCreationBetween(vendeur, debutMois, finMois).stream()
                .filter(l -> l.getStatut() == StatutLivraison.LIVREE)
                .collect(Collectors.toList());

        int nombreLivraisons = livraisonsMois.size();

        // CA du mois = prix produit (COD - frais) des livraisons livrées du mois
        BigDecimal caGenere = livraisonsMois.stream()
                .map(l -> l.getMontantCOD().subtract(l.getFraisLivraison()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fraisLivraison = livraisonsMois.stream()
                .map(Livraison::getFraisLivraison)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Statistiques du mois
        VendeurFinancesStatistiquesMois statsMois = new VendeurFinancesStatistiquesMois();
        statsMois.setNombreLivraisons(nombreLivraisons);
        statsMois.setCaGenere(caGenere);
        statsMois.setFraisLivraison(fraisLivraison);

        // Historique des paiements - CORRECTION ICI
        List<Transaction> transactions = transactionRepository
                .findByVendeurAndTypeAndStatut(
                        vendeur,
                        Transaction.TypeTransaction.PAIEMENT_VENDEUR,
                        Transaction.StatutPaiement.EFFECTUE
                );

        List<VendeurFinancesHistoriquePaiementsInner> historique = transactions.stream()
                .map(this::mapTransactionToHistorique)
                .collect(Collectors.toList());

        // Construire la réponse
        VendeurFinances finances = new VendeurFinances();
        finances.setSoldeEnAttente(soldeEnAttente);
        finances.setChiffreAffaires(chiffreAffaires);
        finances.setStatistiquesMois(statsMois);
        finances.setHistoriquePaiements(historique);

        log.info("✅ Finances récupérées - Solde: {} FCFA, Livraisons mois: {}",
                soldeEnAttente, nombreLivraisons);

        return finances;
    }

    /**
     * Récupère les détails complets d'une livraison
     */
    @Transactional(readOnly = true)
    public LivraisonDetailResponse getDetailLivraison(Long id) {
        log.info("=== Récupération détails livraison {} ===", id);

        // Vendeur vendeur = getCurrentVendeur(); // Admin va utiliser cette endpoint donc on peut pas get le vendeur comme ca
        // on le fait manuellent

        Livraison livraison = livraisonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Livraison non trouvée"));

        Vendeur vendeur = vendeurRepository.findById(livraison.getVendeur().getId()).orElse(null);


        // Vérifier que la livraison appartient bien au vendeur connecté
//        if (!livraison.getVendeur().getId().equals(vendeur.getId())) {
//            throw new IllegalArgumentException("Vous n'avez pas accès à cette livraison");
//        }

        // Construire la réponse détaillée
        LivraisonDetailResponse detail = new LivraisonDetailResponse();
        detail.setId(livraison.getId());
        detail.setNumeroTracking(livraison.getNumeroTracking());
        detail.setQrCodeUrl(livraison.getQrCodeUrl());
        detail.setStatut(sn.votreplateforme.logistique.dto.StatutLivraison.valueOf(
                livraison.getStatut().name()));

        // Dates
        if (livraison.getDateCreation() != null) {
            detail.setDateCreation(livraison.getDateCreation().atOffset(ZoneOffset.UTC));
        }
        if (livraison.getDateRamassage() != null) {
            detail.setDateRamassage(livraison.getDateRamassage().atOffset(ZoneOffset.UTC));
        }
        if (livraison.getDateLivraison() != null) {
            detail.setDateLivraison(livraison.getDateLivraison().atOffset(ZoneOffset.UTC));
        }

        // Informations vendeur
        LivraisonDetailResponseVendeur vendeurInfo = new LivraisonDetailResponseVendeur();
        vendeurInfo.setId(vendeur.getId());
        vendeurInfo.setNom(vendeur.getNom());
        vendeurInfo.setPrenom(vendeur.getPrenom());
        vendeurInfo.setTelephone(vendeur.getTelephone());
        vendeurInfo.setNomBoutique(vendeur.getNomBoutique());
        detail.setVendeur(vendeurInfo);

        // Informations client
        LivraisonDetailResponseClient clientInfo = new LivraisonDetailResponseClient();
        clientInfo.setNom(livraison.getNomClient());
        clientInfo.setTelephone(livraison.getTelephoneClient());
        clientInfo.setAdresse(livraison.getAdresseDestination().getAdresseComplete());
        clientInfo.setPointRepere(livraison.getAdresseDestination().getPointRepere());
        detail.setClient(clientInfo);

        // Informations produit
        LivraisonDetailResponseProduit produitInfo = new LivraisonDetailResponseProduit();
        produitInfo.setDescription(livraison.getDescriptionProduit());
        produitInfo.setFragile(livraison.getFragile());
        produitInfo.setPoids(livraison.getPoids().doubleValue());
        detail.setProduit(produitInfo);

        // Informations financières
        LivraisonDetailResponseFinancier financierInfo = new LivraisonDetailResponseFinancier();
        financierInfo.setMontantCOD(livraison.getMontantCOD());
        financierInfo.setFraisLivraison(livraison.getFraisLivraison());
        financierInfo.setCashCollecte(livraison.getCashCollecte());
        detail.setFinancier(financierInfo);

        // Zone et urgence
        detail.setZone(livraison.getAdresseDestination().getZone().getNom());
        detail.setUrgence(sn.votreplateforme.logistique.dto.TypeUrgence.valueOf(
                livraison.getUrgence().name()));
        detail.setCommentaireLivraison(livraison.getCommentaireLivraison());

        // Livreur assigné (module Dispatch) - null tant que la commande n'est pas assignée
        if (livraison.getLivreur() != null) {
            var l = livraison.getLivreur();
            LivraisonDetailResponseLivreur livreurInfo = new LivraisonDetailResponseLivreur();
            livreurInfo.setId(l.getId());
            livreurInfo.setNom(l.getNom());
            livreurInfo.setPrenom(l.getPrenom());
            livreurInfo.setTelephone(l.getTelephone());
            detail.setLivreur(livreurInfo);
        }

        // Traçabilité qualité (acteurs, horodatage, durées)
        detail.setSuivi(construireSuivi(livraison));

        log.info("✅ Détails livraison {} récupérés", livraison.getNumeroTracking());

        return detail;
    }

    /**
     * Crée une demande de paiement pour le vendeur
     */
    @Transactional
    public VendeurDemandePaiementPost200Response demanderPaiement() {
        log.info("=== Demande de paiement vendeur ===");

        Vendeur vendeur = getCurrentVendeur();

        // Calculer le solde disponible (source unique : FinanceCalculator)
        BigDecimal soldeEnAttente = financeCalculator.soldeDisponible(vendeur);

        // Vérifier qu'il y a un solde à payer
        if (soldeEnAttente.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Aucun solde à récupérer");
        }

        // Vérifier qu'il n'y a pas déjà une demande en attente
        boolean demandeEnCours = transactionRepository
                .existsByVendeurAndTypeAndStatut(
                        vendeur,
                        Transaction.TypeTransaction.PAIEMENT_VENDEUR,
                        Transaction.StatutPaiement.EN_ATTENTE
                );

        if (demandeEnCours) {
            throw new IllegalStateException("Vous avez déjà une demande de paiement en cours");
        }

        // Générer la référence de transaction
        String reference = genererReferenceTransaction();

        // Créer la transaction de demande de paiement
        Transaction transaction = new Transaction();
        transaction.setVendeur(vendeur);
        transaction.setType(Transaction.TypeTransaction.PAIEMENT_VENDEUR);
        transaction.setMontant(soldeEnAttente);
        transaction.setReference(reference);  // ← AJOUT ICI
        transaction.setStatut(Transaction.StatutPaiement.EN_ATTENTE);
        transaction.setCommentaire("Demande de paiement - Solde en attente");

        transaction = transactionRepository.save(transaction);

        log.info("✅ Demande de paiement créée - Ref: {} - Montant: {} FCFA",
                reference, soldeEnAttente);

        // Notifier l'admin (simulation)
        notificationService.envoyerNotificationAdmin(
                "💰 Nouvelle demande de paiement",
                String.format("Vendeur: %s %s\nMontant: %s FCFA\nBoutique: %s\nRéférence: %s",
                        vendeur.getPrenom(), vendeur.getNom(),
                        soldeEnAttente.toString(),
                        vendeur.getNomBoutique(),
                        reference)
        );

        // Construire la réponse
        VendeurDemandePaiementPost200Response response = new VendeurDemandePaiementPost200Response();
        response.setMessage("Demande de paiement envoyée à l'admin");
        response.setMontant(soldeEnAttente);

        return response;
    }
    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Construit le bloc de traçabilité qualité (acteurs, horodatage, durées en minutes).
     */
    private LivraisonDetailResponseSuivi construireSuivi(Livraison l) {
        LivraisonDetailResponseSuivi s = new LivraisonDetailResponseSuivi();

        if (l.getCloseur() != null) {
            LivraisonDetailResponseSuiviCloseur c = new LivraisonDetailResponseSuiviCloseur();
            c.setNom(l.getCloseur().getNom());
            c.setPrenom(l.getCloseur().getPrenom());
            s.setCloseur(c);
        }
        if (l.getDispatcheur() != null) {
            LivraisonDetailResponseSuiviDispatcheur d = new LivraisonDetailResponseSuiviDispatcheur();
            d.setNom(l.getDispatcheur().getNom());
            d.setPrenom(l.getDispatcheur().getPrenom());
            s.setDispatcheur(d);
        }

        s.setDateCreation(offset(l.getDateCreation()));
        s.setDatePriseEnCharge(offset(l.getDatePriseEnCharge()));
        s.setDateConfirmation(offset(l.getDateConfirmation()));
        s.setDatePreteALivrer(offset(l.getDatePreteALivrer()));
        s.setDateAssignation(offset(l.getDateAssignation()));
        s.setDateLivraison(offset(l.getDateLivraison()));
        s.setDateEchec(offset(l.getDateEchec()));

        s.setMinutesPriseEnCharge(minutes(l.getDateCreation(), l.getDatePriseEnCharge()));
        s.setMinutesClosing(minutes(l.getDatePriseEnCharge(), l.getDatePreteALivrer()));
        s.setMinutesDispatch(minutes(l.getDatePreteALivrer(), l.getDateAssignation()));
        s.setMinutesLivraison(minutes(l.getDateAssignation(), l.getDateLivraison()));

        return s;
    }

    private java.time.OffsetDateTime offset(LocalDateTime dt) {
        return dt != null ? dt.atOffset(ZoneOffset.UTC) : null;
    }

    private Integer minutes(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            return null;
        }
        long m = java.time.Duration.between(debut, fin).toMinutes();
        return (int) Math.max(0, m);
    }

    /**
     * Récupère le vendeur actuellement connecté
     */
    private Vendeur getCurrentVendeur() {
        String telephone = SecurityUtils.getCurrentUserTelephone();
        return vendeurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new IllegalStateException("Vendeur non trouvé"));
    }

    /**
     * Convertit une Transaction en HistoriquePaiement DTO
     */
    private VendeurFinancesHistoriquePaiementsInner mapTransactionToHistorique(Transaction transaction) {
        VendeurFinancesHistoriquePaiementsInner historique = new VendeurFinancesHistoriquePaiementsInner();

        if (transaction.getDateTransaction() != null) {
            historique.setDate(transaction.getDateTransaction().atOffset(ZoneOffset.UTC));
        }

        historique.setMontant(transaction.getMontant());
        historique.setReference(transaction.getReference());

        // Convertir le statut
        String statutStr = switch (transaction.getStatut()) {
            case EFFECTUE -> "EFFECTUE";
            case EN_ATTENTE -> "EN_ATTENTE";
            case ANNULE -> "ANNULE";
        };
        historique.setStatut(VendeurFinancesHistoriquePaiementsInner.StatutEnum.fromValue(statutStr));

        return historique;
    }

    /**
     * Génère une référence unique de transaction
     * Format: DEM-YYYYMMDD-XXX
     */
    private String genererReferenceTransaction() {
        String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Compter les transactions du jour
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour = LocalDate.now().atTime(23, 59, 59);

        long nombreTransactionsJour = transactionRepository
                .countByDateTransactionBetween(debutJour, finJour);

        // Incrémenter et formater sur 3 chiffres
        String numero = String.format("%03d", nombreTransactionsJour + 1);

        return "DEM-" + dateStr + "-" + numero;
    }
}