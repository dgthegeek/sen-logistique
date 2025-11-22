package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.AdminRamassagesApi;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.service.RamassageService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller Admin - Ramassages
 * Implémente l'interface AdminRamassagesApi générée par OpenAPI
 * 
 * Endpoints:
 * - GET /admin/ramassages - Liste tous les ramassages
 * - GET /admin/ramassages/today - Ramassages du jour
 * - POST /admin/ramassages/notifier-zone/{zoneNom} - Notifier vendeurs
 * - POST /admin/ramassages/marquer-ramasse - Marquer comme ramassé
 * - POST /admin/ramassages/imprimer-qr - Imprimer QR codes
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminRamassageController implements AdminRamassagesApi {
    
    private final RamassageService ramassageService;
    
    /**
     * GET /admin/ramassages
     * Liste de tous les ramassages groupés par zone
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<AdminRamassagesGet200Response> adminRamassagesGet(LocalDate date, String zone) {
        log.info("=== Requête liste ramassages admin ===");
        log.debug("Date: {}, Zone: {}", date, zone);

        AdminRamassagesGet200Response response = new AdminRamassagesGet200Response();
        
        try {
            Map<String, List<Livraison>> ramassagesParZone = 
                ramassageService.getRamassagesGroupesParZone();
            
            // Filtrer par zone si spécifié
            if (zone != null && !zone.isEmpty()) {
                List<Livraison> livraisonsZone = ramassagesParZone.get(zone);
                if (livraisonsZone == null || livraisonsZone.isEmpty()) {
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
                ramassagesParZone = Map.of(zone, livraisonsZone);
            }
            
            // Calculer le total
            int totalColis = ramassagesParZone.values().stream()
                .mapToInt(List::size)
                .sum();
            
            // Construire la réponse
            response.setTotalColis(totalColis);
            response.setZones(construireZonesResponse(ramassagesParZone));

            log.info("✅ Ramassages récupérés: {} colis dans {} zones", 
                totalColis, ramassagesParZone.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des ramassages", e);
            throw new RuntimeException("Erreur: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Resource> adminRamassagesImprimerQrPost(AdminRamassagesImprimerQrPostRequest adminRamassagesImprimerQrPostRequest) {
        return null;
    }

    @Override
    public ResponseEntity<AdminRamassagesMarquerRamassePost200Response> adminRamassagesMarquerRamassePost(AdminRamassagesMarquerRamassePostRequest adminRamassagesMarquerRamassePostRequest) {
        return null;
    }

    /**
     * GET /admin/ramassages/today
     * Ramassages du jour groupés par zone
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<AdminRamassagesTodayGet200Response> adminRamassagesTodayGet() {
        log.info("=== Requête ramassages du jour ===");
        
        try {
            Map<String, List<Livraison>> ramassagesParZone = 
                ramassageService.getRamassagesAujourdhui();
            
            int totalColis = ramassagesParZone.values().stream()
                .mapToInt(List::size)
                .sum();

            AdminRamassagesTodayGet200Response response = new AdminRamassagesTodayGet200Response();
            response.setTotalColis(totalColis);
            response.setZones(construireZonesResponse(ramassagesParZone));

            log.info("✅ Ramassages du jour: {} colis dans {} zones", 
                totalColis, ramassagesParZone.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des ramassages du jour", e);
            throw new RuntimeException("Erreur: " + e.getMessage());
        }
    }
    
    /**
     * POST /admin/ramassages/notifier-zone/{zoneNom}
     * Notifie les vendeurs d'une zone pour préparer leurs colis
     */
    @Override
    public ResponseEntity<AdminRamassagesNotifierZoneZoneNomPost200Response> adminRamassagesNotifierZoneZoneNomPost(
            String zoneNom,
            AdminRamassagesNotifierZoneZoneNomPostRequest body) {
        
        log.info("=== Requête notification zone: {} ===", zoneNom);
        
        try {
            // Extraire l'heure de ramassage du body (si fourni)
            String heureRamassage = "15h"; // Valeur par défaut
            
            if (body instanceof Map) {
                Map<String, Object> bodyMap = (Map<String, Object>) body;
                if (bodyMap.containsKey("heureRamassage")) {
                    heureRamassage = bodyMap.get("heureRamassage").toString();
                }
            }
            
            // Notifier les vendeurs
            int vendeursNotifies = ramassageService.notifierVendeursZone(zoneNom, heureRamassage);

            AdminRamassagesNotifierZoneZoneNomPost200Response response = new AdminRamassagesNotifierZoneZoneNomPost200Response();
            response.setMessage(vendeursNotifies + " vendeur(s) notifié(s) "+ "| heureRamassage : " + heureRamassage + " | zone: "+ zoneNom);
            response.setVendeursNotifies(vendeursNotifies);

            log.info("✅ {} vendeurs notifiés pour la zone {}", vendeursNotifies, zoneNom);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la notification de la zone {}", zoneNom, e);
            throw new RuntimeException("Erreur: " + e.getMessage());
        }
    }

    /**
     * Construit la réponse avec les zones et leurs ramassages
     */
    private List<RamassageZone> construireZonesResponse(Map<String, List<Livraison>> ramassagesParZone) {
        return ramassagesParZone.entrySet().stream()
                .map(entry -> {
                    String zoneNom = entry.getKey();
                    List<Livraison> livraisons = entry.getValue();

                    // Grouper par vendeur
                    Map<String, List<Livraison>> parVendeur = livraisons.stream()
                            .collect(Collectors.groupingBy(
                                    l -> l.getVendeur().getId().toString()
                            ));

                    // Construire l'objet RamassageZone
                    RamassageZone zone = new RamassageZone();
                    zone.setZone(zoneNom);
                    zone.setNombreColis(livraisons.size());

                    // Construire la liste des vendeurs
                    List<RamassageZoneVendeursInner> vendeursDto =
                            construireVendeursResponse(parVendeur);

                    zone.setVendeurs(vendeursDto);

                    return zone;
                })
                .collect(Collectors.toList());
    }


    /**
     * Construit la réponse avec les vendeurs et leurs colis
     */
    private List<RamassageZoneVendeursInner> construireVendeursResponse(Map<String, List<Livraison>> livraisonsParVendeur) {

        return livraisonsParVendeur.values().stream()
            .map(livraisons -> {
                Livraison premiere = livraisons.getFirst();

                RamassageZoneVendeursInner vendeursDto = new RamassageZoneVendeursInner();
                vendeursDto.setId(premiere.getVendeur().getId());
                vendeursDto.setNom(premiere.getVendeur().getNom());
                vendeursDto.setPrenom(premiere.getVendeur().getPrenom());
                vendeursDto.setTelephone(premiere.getVendeur().getTelephone());
                vendeursDto.setAdresse(premiere.getVendeur().getAdresseComplete());
                vendeursDto.setNombreColis(livraisons.size());
                vendeursDto.setColis(construireColisResponse(livraisons));

                return vendeursDto;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Construit la réponse avec les détails des colis
     */
    private List<RamassageZoneVendeursInnerColisInner> construireColisResponse(List<Livraison> livraisons) {

        return livraisons.stream()
                .map(livraison -> {

                    RamassageZoneVendeursInnerColisInner colisDto =
                            new RamassageZoneVendeursInnerColisInner();

                    colisDto.setId(livraison.getId());
                    colisDto.setNumeroTracking(livraison.getNumeroTracking());
                    colisDto.setNomClient(livraison.getNomClient());
                    colisDto.setAdresseClient(livraison.getAdresseDestination().getAdresseComplete());
                    colisDto.setMontantCOD(livraison.getMontantCOD()); // BigDecimal OK

                    return colisDto;
                })
                .collect(Collectors.toList());
    }

}
