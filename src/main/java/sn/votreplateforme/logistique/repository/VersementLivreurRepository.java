package sn.votreplateforme.logistique.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.VersementLivreur;

import java.util.List;

/**
 * Repository pour l'historique des versements de cash des livreurs.
 */
@Repository
public interface VersementLivreurRepository extends JpaRepository<VersementLivreur, Long> {

    /** Historique global des versements (le plus récent d'abord), paginé. */
    Page<VersementLivreur> findAllByOrderByDateVersementDesc(Pageable pageable);

    /** Historique des versements d'un livreur donné. */
    List<VersementLivreur> findByLivreur_IdOrderByDateVersementDesc(Long livreurId);
}
