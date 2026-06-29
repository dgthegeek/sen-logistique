package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Livreur;

import java.util.Optional;

/**
 * Repository pour l'entité Livreur (module Dispatch)
 */
@Repository
public interface LivreurRepository extends JpaRepository<Livreur, Long> {

    Optional<Livreur> findByTelephone(String telephone);

    Optional<Livreur> findByEmail(String email);

    long countByActifTrue();
}
