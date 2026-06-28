package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Closeur;

import java.util.Optional;

/**
 * Repository pour l'entité Closeur (module Closing)
 */
@Repository
public interface CloseurRepository extends JpaRepository<Closeur, Long> {

    Optional<Closeur> findByTelephone(String telephone);

    Optional<Closeur> findByEmail(String email);
}
