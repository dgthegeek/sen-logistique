package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Quartier;
import sn.votreplateforme.logistique.entity.Zone;
import sn.votreplateforme.logistique.exception.BadRequestException;
import sn.votreplateforme.logistique.exception.NotFoundException;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.QuartierRepository;
import sn.votreplateforme.logistique.repository.ZoneRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuartierAdminService {

    private final QuartierRepository quartierRepository;
    private final ZoneRepository zoneRepository;
    private final LivraisonRepository livraisonRepository;

    /**
     * POST /admin/quartiers
     */
    @Transactional
    public AdminQuartiersPost201Response createQuartier(CreateQuartierRequest request) {
        // Vérifier que le quartier n'existe pas déjà
        if (quartierRepository.findByNomAndCommune(request.getNom(), request.getCommune()).isPresent()) {
            throw new BadRequestException(
                    "Le quartier '" + request.getNom() + "' existe déjà dans la commune " + request.getCommune()
            );
        }

        // Vérifier que la zone existe
        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new NotFoundException("Zone non trouvée"));

        Quartier quartier = new Quartier();
        quartier.setNom(request.getNom());
        quartier.setCommune(request.getCommune());
        quartier.setZone(zone);
        quartier.setActif(request.getActif() != null ? request.getActif() : true);

        quartier = quartierRepository.save(quartier);

        log.info("✅ Quartier créé : {} ({}) - Zone: {}",
                quartier.getNom(), quartier.getCommune(), zone.getNom());

        AdminQuartiersPost201Response response = new AdminQuartiersPost201Response();
        response.setMessage("Quartier créé avec succès");
        response.setQuartier(toQuartierDTO(quartier));

        return response;
    }

    /**
     * PUT /admin/quartiers/{id}
     */
    @Transactional
    public AdminQuartiersIdPut200Response updateQuartier(Long id, UpdateQuartierRequest request) {
        Quartier quartier = quartierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quartier non trouvé"));

        // Vérifier unicité (sauf si c'est le même)
        quartierRepository.findByNomAndCommune(request.getNom(), request.getCommune())
                .ifPresent(q -> {
                    if (!q.getId().equals(id)) {
                        throw new BadRequestException("Un quartier avec ce nom existe déjà dans cette commune");
                    }
                });

        // Vérifier que la zone existe
        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new NotFoundException("Zone non trouvée"));

        quartier.setNom(request.getNom());
        quartier.setCommune(request.getCommune());
        quartier.setZone(zone);
        quartier.setActif(request.getActif());

        quartier = quartierRepository.save(quartier);

        log.info("✅ Quartier modifié : {} ({}) - Zone: {}",
                quartier.getNom(), quartier.getCommune(), zone.getNom());

        AdminQuartiersIdPut200Response response = new AdminQuartiersIdPut200Response();
        response.setMessage("Quartier modifié avec succès");
        response.setQuartier(toQuartierDTO(quartier));

        return response;
    }

    /**
     * DELETE /admin/quartiers/{id}
     */
    @Transactional
    public AdminQuartiersIdDelete200Response deleteQuartier(Long id) {
        Quartier quartier = quartierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quartier non trouvé"));

        // Vérifier qu'il n'y a pas de livraisons
        long nombreLivraisons = livraisonRepository.countByAdresseDestination_Quartier(quartier.getNom());
        if (nombreLivraisons > 0) {
            throw new BadRequestException(
                    "Impossible de supprimer le quartier : " + nombreLivraisons + " livraisons y sont associées"
            );
        }

        quartierRepository.delete(quartier);

        log.warn("🗑️ Quartier supprimé : {} ({})", quartier.getNom(), quartier.getCommune());

        AdminQuartiersIdDelete200Response response = new AdminQuartiersIdDelete200Response();
        response.setMessage("Quartier supprimé avec succès");

        return response;
    }

    /**
     * PATCH /admin/quartiers/{id}/toggle
     */
    @Transactional
    public AdminQuartiersIdTogglePatch200Response toggleQuartier(Long id) {
        Quartier quartier = quartierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quartier non trouvé"));

        quartier.setActif(!quartier.isActif());
        quartier = quartierRepository.save(quartier);

        String action = quartier.isActif() ? "activé" : "désactivé";
        log.info("🔄 Quartier {} : {} ({})", action, quartier.getNom(), quartier.getCommune());

        AdminQuartiersIdTogglePatch200Response response = new AdminQuartiersIdTogglePatch200Response();
        response.setMessage("Quartier " + action + " avec succès");

        AdminZonesIdTogglePatch200ResponseZone quartierDTO = new AdminZonesIdTogglePatch200ResponseZone();
        quartierDTO.setId(quartier.getId());
        quartierDTO.setNom(quartier.getNom());
        quartierDTO.setActif(quartier.isActif());

        response.setQuartier(quartierDTO);

        return response;
    }

    // ===== MAPPER =====

    private QuartierDTO toQuartierDTO(Quartier quartier) {
        QuartierDTO dto = new QuartierDTO();
        dto.setId(quartier.getId());
        dto.setNom(quartier.getNom());
        dto.setCommune(quartier.getCommune());
        dto.setActif(quartier.isActif());

        if (quartier.getZone() != null) {
            QuartierDTOZone zoneDTO = new QuartierDTOZone();
            zoneDTO.setId(quartier.getZone().getId());
            zoneDTO.setNom(quartier.getZone().getNom());
            dto.setZone(zoneDTO);
        }

        return dto;
    }
}