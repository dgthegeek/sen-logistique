package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Dispatcheur;

import java.util.Optional;

/**
 * Repository pour l'entité Dispatcheur (module Dispatch)
 */
@Repository
public interface DispatcheurRepository extends JpaRepository<Dispatcheur, Long> {

    Optional<Dispatcheur> findByTelephone(String telephone);

    Optional<Dispatcheur> findByEmail(String email);

    /** Dispatcheurs actifs ayant lié leur compte Telegram (notification de file). */
    java.util.List<Dispatcheur> findByActifTrueAndTelegramChatIdIsNotNull();
}
