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
     * PAS 1 en dur : elle repart du plus grand numéro déjà existant avec ce
     * préfixe (ex. "DKR-20261030-"), pour ne jamais entrer en collision
     * avec des numéros déjà créés avant l'introduction de ce compteur (ou
     * par tout autre moyen). On utilise MAX, pas COUNT : s'il existe un
     * trou dans la séquence (numéro supprimé, tentative échouée...), COUNT
     * sous-estime le dernier numéro et recrée une collision — vu en prod.
     * Les appels suivants, eux, incrémentent uniquement la ligne déjà
     * créée, sans recalculer.
     */
    @Query(value = "INSERT INTO tracking_number_counters (jour, dernier_numero) " +
            "VALUES (:jour, COALESCE((SELECT MAX(CAST(RIGHT(numero_tracking, 5) AS INTEGER)) " +
            "FROM livraisons WHERE numero_tracking LIKE :prefixLike), 0) + 1) " +
            "ON CONFLICT (jour) DO UPDATE SET dernier_numero = tracking_number_counters.dernier_numero + 1 " +
            "RETURNING dernier_numero", nativeQuery = true)
    Integer incrementerEtRecuperer(@Param("jour") LocalDate jour, @Param("prefixLike") String prefixLike);
}
