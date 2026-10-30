package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.TrackingNumberCounter;

import java.time.LocalDate;

@Repository
public interface TrackingNumberCounterRepository extends JpaRepository<TrackingNumberCounter, LocalDate> {

    /**
     * Incrémente (ou crée) le compteur du jour et renvoie le nouveau numéro,
     * en une seule opération atomique. Postgres sérialise les accès
     * concurrents à la même ligne (jour) : deux appels simultanés ne
     * peuvent jamais recevoir le même numéro.
     */
    @Query(value = "INSERT INTO tracking_number_counters (jour, dernier_numero) VALUES (:jour, 1) " +
            "ON CONFLICT (jour) DO UPDATE SET dernier_numero = tracking_number_counters.dernier_numero + 1 " +
            "RETURNING dernier_numero", nativeQuery = true)
    Integer incrementerEtRecuperer(@Param("jour") LocalDate jour);
}
