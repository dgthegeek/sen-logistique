package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.CommandesParStatut;
import sn.votreplateforme.logistique.dto.DashboardStats;
import sn.votreplateforme.logistique.dto.LivreurStats;
import sn.votreplateforme.logistique.dto.ZoneStats;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.Livreur;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.LivreurRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de statistiques pour le tableau de bord administrateur :
 * compteurs de commandes par statut, performance des livreurs,
 * temps moyen de livraison et statistiques par zone.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StatsService {

    private static final List<StatutLivraison> EN_COURS = List.of(
            StatutLivraison.ASSIGNEE, StatutLivraison.EN_LIVRAISON);

    private final LivraisonRepository livraisonRepository;
    private final LivreurRepository livreurRepository;

    @Transactional(readOnly = true)
    public DashboardStats getDashboard() {
        DashboardStats dashboard = new DashboardStats();
        dashboard.setCommandes(compterParStatut());
        dashboard.setLivreurs(statsLivreurs());
        dashboard.setZones(statsZones());
        dashboard.setTempsMoyenLivraisonMinutes(
                tempsMoyenMinutes(livraisonRepository.findByStatut(StatutLivraison.LIVREE)));
        return dashboard;
    }

    private CommandesParStatut compterParStatut() {
        CommandesParStatut c = new CommandesParStatut();
        c.setNouvelles((int) livraisonRepository.countByStatut(StatutLivraison.NOUVELLE));
        c.setaAppeler((int) livraisonRepository.countByStatut(StatutLivraison.A_APPELER));
        c.setConfirmees((int) livraisonRepository.countByStatut(StatutLivraison.CONFIRMEE));
        c.setPretesALivrer((int) livraisonRepository.countByStatut(StatutLivraison.PRETE_A_LIVRER));
        c.setAssignees((int) livraisonRepository.countByStatut(StatutLivraison.ASSIGNEE));
        c.setEnLivraison((int) livraisonRepository.countByStatut(StatutLivraison.EN_LIVRAISON));
        c.setLivrees((int) livraisonRepository.countByStatut(StatutLivraison.LIVREE));
        long echecs = livraisonRepository.countByStatut(StatutLivraison.ECHEC)
                + livraisonRepository.countByStatut(StatutLivraison.ECHEC_ABSENT)
                + livraisonRepository.countByStatut(StatutLivraison.ECHEC_REFUSE);
        c.setEchecs((int) echecs);
        return c;
    }

    private List<LivreurStats> statsLivreurs() {
        List<LivreurStats> result = new ArrayList<>();
        for (Livreur livreur : livreurRepository.findAll()) {
            long livrees = livraisonRepository.countByLivreur_IdAndStatut(livreur.getId(), StatutLivraison.LIVREE);
            long echecs = livraisonRepository.countByLivreur_IdAndStatut(livreur.getId(), StatutLivraison.ECHEC);
            long enCours = livraisonRepository.countByLivreur_IdAndStatutIn(livreur.getId(), EN_COURS);
            long total = livrees + echecs;

            LivreurStats stats = new LivreurStats();
            stats.setId(livreur.getId());
            stats.setNom(livreur.getNom());
            stats.setPrenom(livreur.getPrenom());
            stats.setLivrees((int) livrees);
            stats.setEchecs((int) echecs);
            stats.setEnCours((int) enCours);
            stats.setTauxReussite(total > 0 ? round1(livrees * 100.0 / total) : 0.0);
            stats.setTempsMoyenMinutes(tempsMoyenMinutes(
                    livraisonRepository.findByLivreur_IdAndStatut(livreur.getId(), StatutLivraison.LIVREE)));
            result.add(stats);
        }
        return result;
    }

    private List<ZoneStats> statsZones() {
        List<ZoneStats> result = new ArrayList<>();
        for (Object[] row : livraisonRepository.statsParZone()) {
            String zone = (String) row[0];
            long nombre = ((Number) row[1]).longValue();
            BigDecimal ca = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            long echecs = ((Number) row[3]).longValue();

            ZoneStats z = new ZoneStats();
            z.setZone(zone);
            z.setNombreLivraisons((int) nombre);
            z.setChiffreAffaires(ca);
            z.setTauxEchec(nombre > 0 ? round1(echecs * 100.0 / nombre) : 0.0);
            result.add(z);
        }
        return result;
    }

    /** Temps moyen (minutes) entre l'assignation et la livraison. */
    private int tempsMoyenMinutes(List<Livraison> livraisons) {
        long totalMinutes = 0;
        int n = 0;
        for (Livraison l : livraisons) {
            if (l.getDateAssignation() != null && l.getDateLivraison() != null) {
                totalMinutes += Duration.between(l.getDateAssignation(), l.getDateLivraison()).toMinutes();
                n++;
            }
        }
        return n > 0 ? (int) (totalMinutes / n) : 0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
