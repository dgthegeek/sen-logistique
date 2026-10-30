package sn.votreplateforme.logistique.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Compteur quotidien pour la génération atomique des numéros de tracking
 * (DKR-YYYYMMDD-NNNNN). Une ligne par jour ; voir
 * {@link sn.votreplateforme.logistique.util.TrackingNumberGenerator}.
 */
@Entity
@Table(name = "tracking_number_counters")
@Getter
@Setter
@NoArgsConstructor
public class TrackingNumberCounter {

    @Id
    private LocalDate jour;

    @Column(name = "dernier_numero", nullable = false)
    private Integer dernierNumero;
}
