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
     *
     * <p>Si le compteur n'existe pas encore pour ce jour (première commande
     * du jour, ou compteur réinitialisé/recréé), la valeur de départ n'est
     * PAS 1 en dur : elle repart du nombre de livraisons déjà existantes
     * avec ce préfixe (ex. "DKR-20261030-"), pour ne jamais entrer en
     * collision avec des numéros déjà créés avant l'introduction de ce
     * compteur (ou par tout autre moyen). Les appels suivants, eux,
     * incrémentent uniquement la ligne déjà créée, sans recompter.
     */
    @Query(value = "INSERT INTO tracking_number_counters (jour, dernier_numero) " +
            "VALUES (:jour, (SELECT COUNT(*) FROM livraisons WHERE numero_tracking LIKE :prefixLike) + 1) " +
            "ON CONFLICT (jour) DO UPDATE SET dernier_numero = tracking_number_counters.dernier_numero + 1 " +
            "RETURNING dernier_numero", nativeQuery = true)
    Integer incrementerEtRecuperer(@Param("jour") LocalDate jour, @Param("prefixLike") String prefixLike);
}
