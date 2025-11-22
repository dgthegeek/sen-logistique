package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.ZonesApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.service.ZoneService;

import java.util.List;

/**
 * Controller pour la gestion des zones et quartiers (endpoints publics)
 *
 * Endpoints :
 * - GET /zones : Liste des zones de livraison
 * - GET /quartiers/commune/{commune} : Quartiers d'une commune
 * - POST /tarifs/calculer : Calcul du tarif de livraison
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ZoneController implements ZonesApi {

    private final ZoneService zoneService;

    /**
     * GET /api/zones
     * Liste toutes les zones de livraison actives avec leurs tarifs
     */
    @Override
    public ResponseEntity<List<Zone>> zonesGet() {
        log.info("Récupération de la liste des zones");

        List<Zone> zones = zoneService.getAllZones();

        log.info("Nombre de zones récupérées : {}", zones.size());
        return ResponseEntity.ok(zones);
    }

    /**
     * GET /api/quartiers/commune/{commune}
     * Liste les quartiers d'une commune (pour auto-complétion)
     *
     * @param commune Nom de la commune (ex: "Dakar")
     */
    @Override
    public ResponseEntity<List<Quartier>> quartiersCommuneCommuneGet(String commune) {
        log.info("Récupération des quartiers de la commune : {}", commune);

        List<Quartier> quartiers = zoneService.getQuartiersByCommune(commune);

        log.info("Nombre de quartiers trouvés : {}", quartiers.size());
        return ResponseEntity.ok(quartiers);
    }


    /**
     * POST /api/tarifs/calculer
     * Calcule le tarif de livraison selon la zone, l'urgence et le poids
     *
     * Body :
     * {
     *   "zoneId": 2,
     *   "urgence": "NORMAL",
     *   "poids": 2.5
     * }
     */
    @Override
    public ResponseEntity<TarifResponse> tarifsCalculerPost(TarifsCalculerPostRequest tarifsCalculerPostRequest) {
        log.info("Calcul de tarif demandé");

        // Extraire les paramètres
        Long zoneId = tarifsCalculerPostRequest.getZoneId();

        // Extraire et convertir l'urgence (DTO TypeUrgence)
        String urgenceStr = String.valueOf(tarifsCalculerPostRequest.getUrgence());
        TypeUrgence urgence = TypeUrgence.valueOf(urgenceStr); // C'est déjà le DTO TypeUrgence

        Double poids = tarifsCalculerPostRequest.getPoids() != null ?
                tarifsCalculerPostRequest.getPoids() : null;

        log.info("Calcul tarif - zoneId: {}, urgence: {}, poids: {}", zoneId, urgence, poids);

        // Calculer le tarif (ZoneService gère la conversion interne)
        TarifResponse tarifResponse = zoneService.calculerTarif(zoneId, urgence, poids);

        log.info("Tarif calculé : {} FCFA", tarifResponse.getMontant());
        return ResponseEntity.ok(tarifResponse);
    }
}