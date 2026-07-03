package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.MouvementStock;
import sn.votreplateforme.logistique.entity.Produit;
import sn.votreplateforme.logistique.entity.TypeMouvement;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.exception.BusinessException;
import sn.votreplateforme.logistique.exception.ForbiddenException;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.repository.MouvementStockRepository;
import sn.votreplateforme.logistique.repository.ProduitRepository;
import sn.votreplateforme.logistique.repository.UserRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;
import sn.votreplateforme.logistique.util.ProduitCodeGenerator;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service du module Stock : produits des partenaires, entrées/sorties,
 * inventaire, alertes de rupture et journal des mouvements.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockService {

    private final ProduitRepository produitRepository;
    private final MouvementStockRepository mouvementRepository;
    private final VendeurRepository vendeurRepository;
    private final UserRepository userRepository;
    private final ProduitCodeGenerator codeGenerator;
    private final QRCodeService qrCodeService;
    private final TelegramService telegramService;

    // ==================== LECTURE ====================

    @Transactional(readOnly = true)
    public PageProduit listProduits(String search, Long vendeurId, Integer page, Integer size) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 200) ? size : 50;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        String s = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Produit> produits;
        if (vendeurId != null) {
            produits = produitRepository.findByVendeurId(vendeurId, pageable);
        } else if (s == null) {
            produits = produitRepository.findAllByOrderByDateCreationDesc(pageable);
        } else {
            produits = produitRepository.rechercher(s, pageable);
        }

        PageProduit response = new PageProduit();
        response.setContent(produits.getContent().stream().map(this::map).collect(Collectors.toList()));
        response.setPage(pageNumber);
        response.setSize(pageSize);
        response.setTotalElements((int) produits.getTotalElements());
        response.setTotalPages(produits.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public ProduitResponse getProduit(Long id) {
        return map(getProduitEntity(id));
    }

    @Transactional(readOnly = true)
    public ProduitResponse scanProduit(String code) {
        Produit p = produitRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun produit avec le code: " + code));
        return map(p);
    }

    @Transactional(readOnly = true)
    public List<ProduitResponse> getAlertes() {
        return produitRepository.findEnAlerte().stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MouvementResponse> getMouvements(Long produitId) {
        getProduitEntity(produitId); // 404 si absent
        return mouvementRepository.findByProduitIdOrderByDateMouvementDesc(produitId)
                .stream().map(this::mapMouvement).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProduitResponse> getProduitsVendeurConnecte() {
        Vendeur vendeur = getVendeurConnecte();
        return produitRepository.findByVendeurIdOrderByNomAsc(vendeur.getId())
                .stream().map(this::map).collect(Collectors.toList());
    }

    /** Le vendeur connecté crée un produit de son catalogue. */
    @Transactional
    public ProduitResponse createProduitVendeur(CreateMonProduitRequest request) {
        Vendeur vendeur = getVendeurConnecte();
        String code = codeGenerator.generate();
        int quantiteInitiale = request.getQuantiteInitiale() != null ? request.getQuantiteInitiale() : 0;

        Produit produit = Produit.builder()
                .code(code)
                .nom(request.getNom())
                .description(request.getDescription())
                .vendeur(vendeur)
                .prixUnitaire(request.getPrixUnitaire())
                .quantiteStock(quantiteInitiale)
                .seuilAlerte(request.getSeuilAlerte() != null ? request.getSeuilAlerte() : 5)
                .qrCodeUrl(qrCodeService.generateProduitQrUrl(code))
                .actif(true)
                .build();
        produit = produitRepository.save(produit);

        if (quantiteInitiale > 0) {
            enregistrerMouvement(produit, TypeMouvement.CREATION, quantiteInitiale,
                    0, quantiteInitiale, null, "Stock initial à la création");
        }
        log.info("Vendeur {} a créé le produit {} ({})", vendeur.getId(), produit.getNom(), code);
        return map(produit);
    }

    /** Le vendeur connecté modifie un produit de son propre catalogue. */
    @Transactional
    public ProduitResponse updateProduitVendeur(Long id, UpdateProduitRequest request) {
        Vendeur vendeur = getVendeurConnecte();
        Produit produit = getProduitEntity(id);
        if (produit.getVendeur() == null || !produit.getVendeur().getId().equals(vendeur.getId())) {
            throw new ForbiddenException("Ce produit ne fait pas partie de votre catalogue");
        }
        if (request.getNom() != null) produit.setNom(request.getNom());
        if (request.getDescription() != null) produit.setDescription(request.getDescription());
        if (request.getPrixUnitaire() != null) produit.setPrixUnitaire(request.getPrixUnitaire());
        if (request.getSeuilAlerte() != null) produit.setSeuilAlerte(request.getSeuilAlerte());
        if (request.getActif() != null) produit.setActif(request.getActif());
        return map(produitRepository.save(produit));
    }

    // ==================== ÉCRITURE ====================

    @Transactional
    public ProduitResponse createProduit(CreateProduitRequest request) {
        Vendeur vendeur = vendeurRepository.findById(request.getVendeurId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Partenaire (vendeur) non trouvé: " + request.getVendeurId()));

        String code = codeGenerator.generate();
        int quantiteInitiale = request.getQuantiteInitiale() != null ? request.getQuantiteInitiale() : 0;

        Produit produit = Produit.builder()
                .code(code)
                .nom(request.getNom())
                .description(request.getDescription())
                .vendeur(vendeur)
                .prixUnitaire(request.getPrixUnitaire())
                .quantiteStock(quantiteInitiale)
                .seuilAlerte(request.getSeuilAlerte() != null ? request.getSeuilAlerte() : 5)
                .qrCodeUrl(qrCodeService.generateProduitQrUrl(code))
                .actif(true)
                .build();

        produit = produitRepository.save(produit);

        if (quantiteInitiale > 0) {
            enregistrerMouvement(produit, TypeMouvement.CREATION, quantiteInitiale,
                    0, quantiteInitiale, null, "Stock initial à la création");
        }

        log.info("Produit créé: {} ({}), stock initial {}", produit.getNom(), code, quantiteInitiale);
        return map(produit);
    }

    @Transactional
    public ProduitResponse updateProduit(Long id, UpdateProduitRequest request) {
        Produit produit = getProduitEntity(id);
        if (request.getNom() != null) produit.setNom(request.getNom());
        if (request.getDescription() != null) produit.setDescription(request.getDescription());
        if (request.getPrixUnitaire() != null) produit.setPrixUnitaire(request.getPrixUnitaire());
        if (request.getSeuilAlerte() != null) produit.setSeuilAlerte(request.getSeuilAlerte());
        if (request.getActif() != null) produit.setActif(request.getActif());
        return map(produitRepository.save(produit));
    }

    @Transactional
    public ProduitResponse entreeStock(Long id, MouvementStockRequest request) {
        if (request.getQuantite() == null || request.getQuantite() < 1) {
            throw new BusinessException("La quantité doit être supérieure à 0");
        }
        Produit produit = getProduitEntity(id);
        int avant = produit.getQuantiteStock();
        produit.ajouterStock(request.getQuantite());
        int apres = produit.getQuantiteStock();
        produitRepository.save(produit);

        enregistrerMouvement(produit, TypeMouvement.ENTREE, request.getQuantite(),
                avant, apres, null, request.getCommentaire());
        log.info("Entrée stock produit {}: {} -> {}", produit.getCode(), avant, apres);
        return map(produit);
    }

    @Transactional
    public ProduitResponse ajusterStock(Long id, AjustementStockRequest request) {
        if (request.getQuantite() == null || request.getQuantite() < 0) {
            throw new BusinessException("La quantité ajustée ne peut pas être négative");
        }
        Produit produit = getProduitEntity(id);
        int avant = produit.getQuantiteStock();
        int apres = request.getQuantite();
        int variation = apres - avant;
        produit.setQuantiteStock(apres);
        produitRepository.save(produit);

        enregistrerMouvement(produit, TypeMouvement.AJUSTEMENT, variation,
                avant, apres, null,
                request.getCommentaire() != null ? request.getCommentaire() : "Ajustement inventaire");
        log.info("Ajustement stock produit {}: {} -> {}", produit.getCode(), avant, apres);
        return map(produit);
    }

    /**
     * Décrément automatique du stock pour une livraison livrée :
     * gère le multi-produits (lignes de commande) ou le produit unique (compat).
     */
    @Transactional
    public void enregistrerSortiesLivraison(sn.votreplateforme.logistique.entity.Livraison livraison) {
        if (livraison.getLignes() != null && !livraison.getLignes().isEmpty()) {
            for (sn.votreplateforme.logistique.entity.LigneCommande ligne : livraison.getLignes()) {
                if (ligne.getProduit() != null && ligne.getQuantite() != null && ligne.getQuantite() > 0) {
                    enregistrerSortieLivraison(ligne.getProduit().getId(), ligne.getQuantite(), livraison.getId());
                }
            }
        } else if (livraison.getProduit() != null && livraison.getQuantite() != null && livraison.getQuantite() > 0) {
            enregistrerSortieLivraison(livraison.getProduit().getId(), livraison.getQuantite(), livraison.getId());
        }
    }

    /**
     * Sortie de stock automatique lors d'une livraison effectuée.
     * Utilisé par le module Livraison (décrément à la livraison).
     */
    @Transactional
    public void enregistrerSortieLivraison(Long produitId, int quantite, Long livraisonId) {
        Produit produit = getProduitEntity(produitId);
        int avant = produit.getQuantiteStock();
        produit.retirerStock(quantite);
        int apres = produit.getQuantiteStock();
        produitRepository.save(produit);

        enregistrerMouvement(produit, TypeMouvement.SORTIE, -quantite,
                avant, apres, livraisonId, "Livraison effectuée");
        log.info("Sortie stock produit {} (livraison {}): {} -> {}",
                produit.getCode(), livraisonId, avant, apres);

        // Alerte stock : notifier le vendeur quand le stock passe sous le seuil
        int seuil = produit.getSeuilAlerte() != null ? produit.getSeuilAlerte() : 0;
        if (avant > seuil && apres <= seuil) {
            String msg = apres == 0
                    ? String.format("🚨 <b>Rupture de stock</b>%n%s (%s) : stock épuisé.",
                        produit.getNom(), produit.getCode())
                    : String.format("⚠️ <b>Stock faible</b>%n%s (%s) : il reste %d unité(s) (seuil %d).",
                        produit.getNom(), produit.getCode(), apres, seuil);
            telegramService.notifyVendeur(produit.getVendeur(), msg);
        }
    }

    // ==================== HELPERS ====================

    private void enregistrerMouvement(Produit produit, TypeMouvement type, int variation,
                                      int stockAvant, int stockApres, Long livraisonId, String commentaire) {
        MouvementStock mouvement = MouvementStock.builder()
                .produit(produit)
                .type(type)
                .variation(variation)
                .stockAvant(stockAvant)
                .stockApres(stockApres)
                .livraisonId(livraisonId)
                .commentaire(commentaire)
                .auteur(auteurCourant())
                .build();
        mouvementRepository.save(mouvement);
    }

    private String auteurCourant() {
        String tel = SecurityUtils.getCurrentUserTelephone();
        if (tel == null) return "Système";
        return userRepository.findByTelephone(tel)
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse("Système");
    }

    private Produit getProduitEntity(Long id) {
        return produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + id));
    }

    private Vendeur getVendeurConnecte() {
        String tel = SecurityUtils.getCurrentUserTelephone();
        if (tel == null) throw new ForbiddenException("Utilisateur non authentifié");
        return vendeurRepository.findByTelephone(tel)
                .orElseThrow(() -> new ForbiddenException("Aucun partenaire associé à ce compte"));
    }

    private ProduitResponse map(Produit p) {
        ProduitResponse r = new ProduitResponse();
        r.setId(p.getId());
        r.setCode(p.getCode());
        r.setNom(p.getNom());
        r.setDescription(p.getDescription());
        if (p.getVendeur() != null) {
            r.setVendeurId(p.getVendeur().getId());
            r.setVendeurNom(p.getVendeur().getNomComplet());
        }
        r.setPrixUnitaire(p.getPrixUnitaire());
        r.setQuantiteStock(p.getQuantiteStock());
        r.setSeuilAlerte(p.getSeuilAlerte());
        r.setQrCodeUrl(p.getQrCodeUrl());
        r.setActif(p.isActif());
        r.setEnAlerte(p.enAlerte());
        if (p.getDateCreation() != null) {
            r.setDateCreation(p.getDateCreation().atOffset(ZoneOffset.UTC));
        }
        return r;
    }

    private MouvementResponse mapMouvement(MouvementStock m) {
        MouvementResponse r = new MouvementResponse();
        r.setId(m.getId());
        r.setType(MouvementResponse.TypeEnum.valueOf(m.getType().name()));
        r.setVariation(m.getVariation());
        r.setStockAvant(m.getStockAvant());
        r.setStockApres(m.getStockApres());
        r.setLivraisonId(m.getLivraisonId());
        r.setCommentaire(m.getCommentaire());
        r.setAuteur(m.getAuteur());
        if (m.getDateMouvement() != null) {
            r.setDateMouvement(m.getDateMouvement().atOffset(ZoneOffset.UTC));
        }
        return r;
    }
}
