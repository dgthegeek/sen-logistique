package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.BilanJour;
import sn.votreplateforme.logistique.dto.StatutVendeur;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.LivreurRepository;
import sn.votreplateforme.logistique.repository.ProduitRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de bilan quotidien / KPI CEO.
 * Fournit un récapitulatif de l'activité d'une journée et un job planifié
 * qui journalise le bilan chaque soir.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BilanService {

    private final LivraisonRepository livraisonRepository;
    private final VendeurRepository vendeurRepository;
    private final LivreurRepository livreurRepository;
    private final ProduitRepository produitRepository;

    @Transactional(readOnly = true)
    public BilanJour getBilan(LocalDate date) {
        LocalDate jour = (date != null) ? date : LocalDate.now();
        LocalDateTime debut = jour.atStartOfDay();
        LocalDateTime fin = jour.atTime(23, 59, 59);

        List<Livraison> creees = livraisonRepository.findByDateCreationBetween(debut, fin);
        List<Livraison> livrees = livraisonRepository.findLivraisonsDuJour(debut, fin);

        int nbLivrees = livrees.size();
        // Échecs survenus ce jour (par date d'échec), cohérent avec les livraisons du jour
        int nbEchecs = (int) livraisonRepository.countByStatutAndDateEchecBetween(
                StatutLivraison.ECHEC, debut, fin);

        BigDecimal ca = BigDecimal.ZERO;
        BigDecimal benefice = BigDecimal.ZERO;
        long totalMinutes = 0;
        int nbDelai = 0;
        for (Livraison l : livrees) {
            BigDecimal montant = l.getCashCollecte() != null ? l.getCashCollecte() : l.getMontantCOD();
            if (montant != null) ca = ca.add(montant);
            if (l.getFraisLivraison() != null) benefice = benefice.add(l.getFraisLivraison());
            if (l.getDateAssignation() != null && l.getDateLivraison() != null) {
                totalMinutes += Duration.between(l.getDateAssignation(), l.getDateLivraison()).toMinutes();
                nbDelai++;
            }
        }

        int totalTermine = nbLivrees + nbEchecs;
        double taux = totalTermine > 0 ? Math.round(nbLivrees * 1000.0 / totalTermine) / 10.0 : 0.0;

        BilanJour bilan = new BilanJour();
        bilan.setDate(jour);
        bilan.setCommandesCreees(creees.size());
        bilan.setLivrees(nbLivrees);
        bilan.setEchecs(nbEchecs);
        bilan.setTauxReussite(taux);
        bilan.setChiffreAffaires(ca);
        bilan.setBeneficeEstime(benefice);
        bilan.setMontantDuPartenaires(vendeurRepository.sumSoldeEnAttente());
        bilan.setDelaiMoyenMinutes(nbDelai > 0 ? (int) (totalMinutes / nbDelai) : 0);
        bilan.setPartenairesActifs(vendeurRepository.findByStatut(StatutVendeur.ACTIF).size());
        bilan.setLivreursActifs((int) livreurRepository.countByActifTrue());
        bilan.setStockTotalRestant((int) produitRepository.sumStockTotal());
        bilan.setProduitsEnAlerte(produitRepository.findEnAlerte().size());
        return bilan;
    }

    /**
     * Bilan automatique du soir : journalise le récapitulatif chaque jour à 20h00.
     */
    @Scheduled(cron = "0 0 20 * * *")
    public void bilanAutomatiqueDuSoir() {
        BilanJour b = getBilan(LocalDate.now());
        log.info("📊 BILAN DU SOIR {} | commandes: {} | livrées: {} | échecs: {} | CA: {} | bénéfice: {} | " +
                        "dû partenaires: {} | stock restant: {} | alertes: {}",
                b.getDate(), b.getCommandesCreees(), b.getLivrees(), b.getEchecs(),
                b.getChiffreAffaires(), b.getBeneficeEstime(), b.getMontantDuPartenaires(),
                b.getStockTotalRestant(), b.getProduitsEnAlerte());
    }
}
