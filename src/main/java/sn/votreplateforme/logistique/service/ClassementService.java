package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.ClassementEntry;
import sn.votreplateforme.logistique.dto.ClassementResponse;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dioks League : classement gamifié entre vendeurs.
 * Le classement est fondé sur le nombre de livraisons livrées (cumulé, calculé à la
 * volée) : quitter/rejoindre ne remet jamais les données à zéro.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClassementService {

    private final VendeurRepository vendeurRepository;
    private final LivraisonRepository livraisonRepository;

    // ==================== VENDEUR ====================

    @Transactional(readOnly = true)
    public ClassementResponse getClassementVendeur() {
        Vendeur courant = getCurrentVendeur();
        return construire(courant, courant.isParticipeClassement());
    }

    @Transactional
    public ClassementResponse rejoindre() {
        Vendeur courant = getCurrentVendeur();
        if (!courant.isParticipeClassement()) {
            courant.setParticipeClassement(true);
            courant.setDateAdhesionClassement(LocalDateTime.now());
            vendeurRepository.save(courant);
            log.info("Vendeur {} a rejoint la Dioks League", courant.getId());
        }
        return construire(courant, true);
    }

    @Transactional
    public ClassementResponse quitter() {
        Vendeur courant = getCurrentVendeur();
        if (courant.isParticipeClassement()) {
            courant.setParticipeClassement(false);
            vendeurRepository.save(courant);
            log.info("Vendeur {} a quitté la Dioks League (données conservées)", courant.getId());
        }
        return construire(courant, false);
    }

    // ==================== ADMIN ====================

    @Transactional(readOnly = true)
    public ClassementResponse getClassementAdmin() {
        Map<Long, long[]> counts = new HashMap<>();
        Map<Long, BigDecimal> cas = chargerStats(counts);

        List<Vendeur> tous = vendeurRepository.findAll();
        List<ClassementEntry> entries = construireEntries(tous, counts, cas, null, true);

        ClassementResponse resp = new ClassementResponse();
        resp.setParticipe(true);
        resp.setEntries(entries);
        resp.setTotalParticipants((int) tous.stream().filter(Vendeur::isParticipeClassement).count());
        resp.setMesLivraisons(0);
        resp.setProgressionPourcent(0);
        return resp;
    }

    // ==================== CONSTRUCTION ====================

    private ClassementResponse construire(Vendeur courant, boolean participe) {
        Map<Long, long[]> counts = new HashMap<>();
        Map<Long, BigDecimal> cas = chargerStats(counts);

        long mesLivraisons = counts.containsKey(courant.getId()) ? counts.get(courant.getId())[0] : 0;
        BigDecimal monCa = cas.getOrDefault(courant.getId(), BigDecimal.ZERO);
        TierClassement monTier = TierClassement.forLivraisons(mesLivraisons);

        ClassementResponse resp = new ClassementResponse();
        resp.setParticipe(participe);
        resp.setMesLivraisons((int) mesLivraisons);
        resp.setMonChiffreAffaires(monCa);
        resp.monTier(ClassementResponse.MonTierEnum.fromValue(monTier.name()));

        // Progression vers le prochain tier
        TierClassement suivant = monTier.suivant();
        if (suivant == null) {
            resp.livraisonsPourProchainTier(0);
            resp.setProgressionPourcent(100);
        } else {
            resp.prochainTier(ClassementResponse.ProchainTierEnum.fromValue(suivant.name()));
            int restant = (int) Math.max(0, suivant.getSeuil() - mesLivraisons);
            resp.livraisonsPourProchainTier(restant);
            int span = suivant.getSeuil() - monTier.getSeuil();
            int fait = (int) (mesLivraisons - monTier.getSeuil());
            int pct = span > 0 ? Math.max(0, Math.min(100, Math.round(fait * 100f / span))) : 0;
            resp.setProgressionPourcent(pct);
        }

        // Le classement des autres n'est visible qu'aux participants (consentement mutuel)
        if (participe) {
            List<Vendeur> participants = vendeurRepository.findByParticipeClassementTrue();
            List<ClassementEntry> entries = construireEntries(participants, counts, cas, courant.getId(), false);
            resp.setEntries(entries);
            resp.setTotalParticipants(participants.size());
            entries.stream()
                    .filter(e -> Boolean.TRUE.equals(e.getMoi()))
                    .findFirst()
                    .ifPresent(e -> resp.monRang(e.getRang()));
        } else {
            resp.setTotalParticipants(vendeurRepository.findByParticipeClassementTrue().size());
        }

        return resp;
    }

    /** Charge le nombre de livraisons + CA par vendeur. Remplit counts, retourne les CA. */
    private Map<Long, BigDecimal> chargerStats(Map<Long, long[]> countsOut) {
        Map<Long, BigDecimal> cas = new HashMap<>();
        for (Object[] row : livraisonRepository.statsLivreesParVendeur()) {
            Long vendeurId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            BigDecimal ca = (BigDecimal) row[2];
            countsOut.put(vendeurId, new long[]{count});
            cas.put(vendeurId, ca != null ? ca : BigDecimal.ZERO);
        }
        return cas;
    }

    private List<ClassementEntry> construireEntries(List<Vendeur> vendeurs,
                                                    Map<Long, long[]> counts,
                                                    Map<Long, BigDecimal> cas,
                                                    Long courantId,
                                                    boolean admin) {
        List<ClassementEntry> entries = vendeurs.stream().map(v -> {
            long n = counts.containsKey(v.getId()) ? counts.get(v.getId())[0] : 0;
            BigDecimal ca = cas.getOrDefault(v.getId(), BigDecimal.ZERO);
            boolean moi = courantId != null && courantId.equals(v.getId());

            ClassementEntry e = new ClassementEntry();
            e.setVendeurId(v.getId());
            e.setNomAffiche(nomAffiche(v));
            e.setNombreLivraisons((int) n);
            // Le CA des autres reste privé côté vendeur ; l'admin (et soi-même) le voient.
            e.setChiffreAffaires((admin || moi) ? ca : null);
            e.setTier(ClassementEntry.TierEnum.fromValue(TierClassement.forLivraisons(n).name()));
            e.setMoi(moi);
            e.setParticipe(v.isParticipeClassement());
            return e;
        }).sorted(
                Comparator.comparingInt(ClassementEntry::getNombreLivraisons).reversed()
                        .thenComparing(e -> nomAfficheSafe(e))
        ).collect(Collectors.toList());

        int rang = 1;
        for (ClassementEntry e : entries) {
            e.setRang(rang++);
        }
        return entries;
    }

    private String nomAffiche(Vendeur v) {
        if (v.getNomBoutique() != null && !v.getNomBoutique().isBlank()) {
            return v.getNomBoutique();
        }
        return ((v.getPrenom() != null ? v.getPrenom() : "") + " "
                + (v.getNom() != null ? v.getNom() : "")).trim();
    }

    private String nomAfficheSafe(ClassementEntry e) {
        return e.getNomAffiche() != null ? e.getNomAffiche() : "";
    }

    private Vendeur getCurrentVendeur() {
        String telephone = SecurityUtils.getCurrentUserTelephone();
        return vendeurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new IllegalStateException("Vendeur non trouvé"));
    }
}
