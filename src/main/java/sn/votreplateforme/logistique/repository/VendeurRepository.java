package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Vendeur;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Vendeur
 */
@Repository
public interface VendeurRepository extends JpaRepository<Vendeur, Long> {

    Optional<Vendeur> findByTelephone(String telephone);

    Optional<Vendeur> findByEmail(String email);

    List<Vendeur> findByActifTrue();

    List<Vendeur> findByCommune(String commune);

    List<Vendeur> findByQuartier(String quartier);

    List<Vendeur> findBySoldeEnAttenteGreaterThanEqual(BigDecimal montant);

    long countByActifTrue();

    @Query("SELECT COALESCE(SUM(v.soldeEnAttente), 0) FROM Vendeur v WHERE v.actif = true")
    BigDecimal calculerSoldeTotalEnAttente();

    List<Vendeur> findBySoldeEnAttenteGreaterThan(BigDecimal montant);
}
