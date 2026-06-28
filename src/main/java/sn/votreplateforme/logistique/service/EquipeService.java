package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.CreateMembreRequest;
import sn.votreplateforme.logistique.dto.LivreurResponse;
import sn.votreplateforme.logistique.dto.MembreResponse;
import sn.votreplateforme.logistique.entity.Closeur;
import sn.votreplateforme.logistique.entity.Livreur;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.entity.UserRole;
import sn.votreplateforme.logistique.repository.CloseurRepository;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.LivreurRepository;
import sn.votreplateforme.logistique.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestion des comptes de l'équipe : closeurs et livreurs.
 * Réservé à l'administrateur.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EquipeService {

    private static final List<StatutLivraison> EN_COURS = List.of(
            StatutLivraison.ASSIGNEE,
            StatutLivraison.EN_LIVRAISON
    );

    private final CloseurRepository closeurRepository;
    private final LivreurRepository livreurRepository;
    private final UserRepository userRepository;
    private final LivraisonRepository livraisonRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== CLOSEURS ====================

    @Transactional
    public MembreResponse createCloseur(CreateMembreRequest request) {
        verifierUnicite(request);

        Closeur closeur = new Closeur();
        closeur.setNom(request.getNom());
        closeur.setPrenom(request.getPrenom());
        closeur.setTelephone(request.getTelephone());
        closeur.setEmail(request.getEmail());
        closeur.setPassword(passwordEncoder.encode(request.getPassword()));
        closeur.setRole(UserRole.CLOSEUR);
        closeur.setActif(true);

        closeur = closeurRepository.save(closeur);
        log.info("Closeur créé: {} (ID: {})", closeur.getTelephone(), closeur.getId());
        return mapToMembreResponse(closeur, UserRole.CLOSEUR);
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> listCloseurs() {
        return closeurRepository.findAll().stream()
                .map(c -> mapToMembreResponse(c, UserRole.CLOSEUR))
                .collect(Collectors.toList());
    }

    // ==================== LIVREURS ====================

    @Transactional
    public LivreurResponse createLivreur(CreateMembreRequest request) {
        verifierUnicite(request);

        Livreur livreur = new Livreur();
        livreur.setNom(request.getNom());
        livreur.setPrenom(request.getPrenom());
        livreur.setTelephone(request.getTelephone());
        livreur.setEmail(request.getEmail());
        livreur.setPassword(passwordEncoder.encode(request.getPassword()));
        livreur.setRole(UserRole.LIVREUR);
        livreur.setActif(true);
        livreur.setZonePreferee(request.getZonePreferee());

        livreur = livreurRepository.save(livreur);
        log.info("Livreur créé: {} (ID: {})", livreur.getTelephone(), livreur.getId());
        return mapToLivreurResponse(livreur);
    }

    @Transactional(readOnly = true)
    public List<LivreurResponse> listLivreurs() {
        return livreurRepository.findAll().stream()
                .map(this::mapToLivreurResponse)
                .collect(Collectors.toList());
    }

    // ==================== HELPERS ====================

    private void verifierUnicite(CreateMembreRequest request) {
        if (userRepository.findByTelephone(request.getTelephone()).isPresent()) {
            throw new IllegalArgumentException("Un compte existe déjà avec ce numéro de téléphone");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }
    }

    private MembreResponse mapToMembreResponse(sn.votreplateforme.logistique.entity.User u, UserRole role) {
        MembreResponse m = new MembreResponse();
        m.setId(u.getId());
        m.setNom(u.getNom());
        m.setPrenom(u.getPrenom());
        m.setTelephone(u.getTelephone());
        m.setEmail(u.getEmail());
        m.setRole(sn.votreplateforme.logistique.dto.UserRole.valueOf(role.name()));
        m.setActif(u.isActif());
        return m;
    }

    private LivreurResponse mapToLivreurResponse(Livreur l) {
        LivreurResponse r = new LivreurResponse();
        r.setId(l.getId());
        r.setNom(l.getNom());
        r.setPrenom(l.getPrenom());
        r.setTelephone(l.getTelephone());
        r.setEmail(l.getEmail());
        r.setZonePreferee(l.getZonePreferee());
        r.setActif(l.isActif());
        r.setNombreLivraisonsEnCours(
                (int) livraisonRepository.countByLivreur_IdAndStatutIn(l.getId(), EN_COURS));
        return r;
    }
}
