package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.PerfCloseur;
import sn.votreplateforme.logistique.dto.PerfDispatcheur;
import sn.votreplateforme.logistique.dto.PerfLivreur;
import sn.votreplateforme.logistique.dto.PerformanceResponse;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.repository.LivraisonRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôle qualité : agrège les performances de l'équipe (closeurs, dispatcheurs, livreurs)
 * à partir des horodatages de traçabilité des commandes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PerformanceService {

    private final LivraisonRepository livraisonRepository;

    @Transactional(readOnly = true)
    public PerformanceResponse getPerformance(String periode, LocalDate debut, LocalDate fin) {
        String p = (periode == null || periode.isBlank()) ? "mois" : periode.toLowerCase();

        List<Livraison> livraisons;
        if (debut != null && fin != null) {
            // Plage de dates explicite (comme le bilan) : prime sur la période prédéfinie
            LocalDateTime from = debut.atStartOfDay();
            LocalDateTime to = fin.plusDays(1).atStartOfDay(); // fin incluse
            livraisons = livraisonRepository.findByDateCreationBetween(from, to);
            p = debut + " → " + fin;
        } else if ("tout".equals(p)) {
            livraisons = livraisonRepository.findAll();
        } else {
            LocalDateTime from = switch (p) {
                case "jour" -> LocalDate.now().atStartOfDay();
                case "semaine" -> LocalDate.now().minusDays(7).atStartOfDay();
                default -> LocalDate.now().minusDays(30).atStartOfDay();
            };
            livraisons = livraisonRepository.findByDateCreationBetween(from, LocalDateTime.now());
        }

        Map<Long, CloseurAcc> closeurs = new LinkedHashMap<>();
        Map<Long, DispatchAcc> dispatcheurs = new LinkedHashMap<>();
        Map<Long, LivreurAcc> livreurs = new LinkedHashMap<>();

        long sumPEC = 0, cntPEC = 0;
        long sumDispatch = 0, cntDispatch = 0;
        long sumLivraison = 0, cntLivraison = 0;
        int totalLivrees = 0, totalEchecs = 0;

        for (Livraison l : livraisons) {
            Integer mPEC = minutes(l.getDateCreation(), l.getDatePriseEnCharge());
            Integer mClosing = minutes(l.getDatePriseEnCharge(), l.getDatePreteALivrer());
            Integer mDispatch = minutes(l.getDatePreteALivrer(), l.getDateAssignation());
            Integer mLivraison = minutes(l.getDateAssignation(), l.getDateLivraison());

            // Closeur
            if (l.getCloseur() != null) {
                CloseurAcc a = closeurs.computeIfAbsent(l.getCloseur().getId(), k -> {
                    CloseurAcc na = new CloseurAcc();
                    na.nom = l.getCloseur().getNom();
                    na.prenom = l.getCloseur().getPrenom();
                    return na;
                });
                if (l.getDatePriseEnCharge() != null) a.prisEnCharge++;
                if (l.getDatePreteALivrer() != null) a.pretes++;
                if (mPEC != null) { a.sumPEC += mPEC; a.cntPEC++; }
                if (mClosing != null) { a.sumClosing += mClosing; a.cntClosing++; }
            }

            // Dispatcheur
            if (l.getDispatcheur() != null) {
                DispatchAcc a = dispatcheurs.computeIfAbsent(l.getDispatcheur().getId(), k -> {
                    DispatchAcc na = new DispatchAcc();
                    na.nom = l.getDispatcheur().getNom();
                    na.prenom = l.getDispatcheur().getPrenom();
                    return na;
                });
                if (l.getDateAssignation() != null) a.dispatchees++;
                if (mDispatch != null) { a.sumDispatch += mDispatch; a.cntDispatch++; }
            }

            // Livreur
            if (l.getLivreur() != null) {
                LivreurAcc a = livreurs.computeIfAbsent(l.getLivreur().getId(), k -> {
                    LivreurAcc na = new LivreurAcc();
                    na.nom = l.getLivreur().getNom();
                    na.prenom = l.getLivreur().getPrenom();
                    return na;
                });
                if (l.getStatut() == StatutLivraison.LIVREE) {
                    a.livrees++;
                    if (mLivraison != null) { a.sumLivraison += mLivraison; a.cntLivraison++; }
                } else if (estEchec(l.getStatut())) {
                    a.echecs++;
                }
            }

            // Global
            if (mPEC != null) { sumPEC += mPEC; cntPEC++; }
            if (mDispatch != null) { sumDispatch += mDispatch; cntDispatch++; }
            if (mLivraison != null) { sumLivraison += mLivraison; cntLivraison++; }
            if (l.getStatut() == StatutLivraison.LIVREE) totalLivrees++;
            else if (estEchec(l.getStatut())) totalEchecs++;
        }

        PerformanceResponse resp = new PerformanceResponse();
        resp.setPeriode(p);
        resp.setTempsMoyenPriseEnChargeMin(moyenne(sumPEC, cntPEC));
        resp.setTempsMoyenDispatchMin(moyenne(sumDispatch, cntDispatch));
        resp.setTempsMoyenLivraisonMin(moyenne(sumLivraison, cntLivraison));
        resp.setTotalLivrees(totalLivrees);
        resp.setTotalEchecs(totalEchecs);

        List<PerfCloseur> lc = new ArrayList<>();
        closeurs.forEach((id, a) -> {
            PerfCloseur c = new PerfCloseur();
            c.setId(id); c.setNom(a.nom); c.setPrenom(a.prenom);
            c.setNombrePrisEnCharge(a.prisEnCharge);
            c.setNombrePretes(a.pretes);
            c.setTempsMoyenPriseEnChargeMin(moyenne(a.sumPEC, a.cntPEC));
            c.setTempsMoyenClosingMin(moyenne(a.sumClosing, a.cntClosing));
            lc.add(c);
        });
        lc.sort(Comparator.comparingInt(PerfCloseur::getNombrePrisEnCharge).reversed());
        resp.setCloseurs(lc);

        List<PerfDispatcheur> ld = new ArrayList<>();
        dispatcheurs.forEach((id, a) -> {
            PerfDispatcheur d = new PerfDispatcheur();
            d.setId(id); d.setNom(a.nom); d.setPrenom(a.prenom);
            d.setNombreDispatchees(a.dispatchees);
            d.setTempsMoyenDispatchMin(moyenne(a.sumDispatch, a.cntDispatch));
            ld.add(d);
        });
        ld.sort(Comparator.comparingInt(PerfDispatcheur::getNombreDispatchees).reversed());
        resp.setDispatcheurs(ld);

        List<PerfLivreur> ll = new ArrayList<>();
        livreurs.forEach((id, a) -> {
            PerfLivreur v = new PerfLivreur();
            v.setId(id); v.setNom(a.nom); v.setPrenom(a.prenom);
            v.setNombreLivrees(a.livrees);
            v.setNombreEchecs(a.echecs);
            int total = a.livrees + a.echecs;
            v.setTauxReussite(total > 0 ? Math.round(a.livrees * 1000.0 / total) / 10.0 : 0.0);
            v.setTempsMoyenLivraisonMin(moyenne(a.sumLivraison, a.cntLivraison));
            ll.add(v);
        });
        ll.sort(Comparator.comparingInt(PerfLivreur::getNombreLivrees).reversed());
        resp.setLivreurs(ll);

        log.info("Performance ({}): {} closeur(s), {} dispatcheur(s), {} livreur(s)",
                p, lc.size(), ld.size(), ll.size());
        return resp;
    }

    // ==================== HELPERS ====================

    private boolean estEchec(StatutLivraison s) {
        return s == StatutLivraison.ECHEC || s == StatutLivraison.ECHEC_ABSENT || s == StatutLivraison.ECHEC_REFUSE;
    }

    private Integer minutes(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) return null;
        return (int) Math.max(0, Duration.between(debut, fin).toMinutes());
    }

    private int moyenne(long somme, long compte) {
        return compte > 0 ? (int) (somme / compte) : 0;
    }

    private static class CloseurAcc {
        String nom, prenom;
        int prisEnCharge, pretes;
        long sumPEC, cntPEC, sumClosing, cntClosing;
    }

    private static class DispatchAcc {
        String nom, prenom;
        int dispatchees;
        long sumDispatch, cntDispatch;
    }

    private static class LivreurAcc {
        String nom, prenom;
        int livrees, echecs;
        long sumLivraison, cntLivraison;
    }
}
