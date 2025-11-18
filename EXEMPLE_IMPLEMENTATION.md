# 💡 Exemple Concret - Implémentation d'un Endpoint

Ce fichier montre **pas à pas** comment implémenter un endpoint avec le workflow OpenAPI Generator.

---

## 🎯 Objectif : Implémenter "Créer une livraison"

Endpoint : `POST /api/vendeur/livraisons`

---

## 📋 Étape 1 : L'endpoint existe déjà dans OpenAPI ✅

Dans `src/main/resources/openapi.yaml` :

```yaml
/vendeur/livraisons:
  post:
    tags:
      - Vendeur
    summary: Créer une nouvelle livraison
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/CreateLivraisonRequest'
    responses:
      '201':
        description: Livraison créée avec succès
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LivraisonResponse'
```

---

## 🔨 Étape 2 : Générer le code

```bash
mvn generate-sources
```

**Ce qui est généré automatiquement :**

### 📦 DTO : `CreateLivraisonRequest.java`
```java
// Fichier : target/generated-sources/openapi/sn/votreplateforme/logistique/dto/CreateLivraisonRequest.java
package sn.votreplateforme.logistique.dto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLivraisonRequest {
    
    @NotBlank
    private String nomClient;
    
    @NotBlank
    @Pattern(regexp = "^(77|78|76|70|75)[0-9]{7}$")
    private String telephoneClient;
    
    @NotBlank
    private String commune;
    
    @NotBlank
    private String quartier;
    
    @NotBlank
    private String adresseComplete;
    
    private String pointRepere;
    
    @NotBlank
    private String descriptionProduit;
    
    private Boolean fragile;
    
    private Double poids;
    
    @NotNull
    @DecimalMin("1000")
    @DecimalMax("10000000")
    private BigDecimal montantCOD;
    
    @NotNull
    private Long zoneId;
    
    private TypeUrgence urgence;
    
    private String creneauSouhaite;
    
    private String notesPourLivreur;
}
```

### 📦 DTO : `LivraisonResponse.java`
```java
// Fichier : target/generated-sources/openapi/sn/votreplateforme/logistique/dto/LivraisonResponse.java
package sn.votreplateforme.logistique.dto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivraisonResponse {
    private Long id;
    private String numeroTracking;
    private String qrCodeUrl;
    private StatutLivraison statut;
    private OffsetDateTime dateCreation;
    private BigDecimal fraisLivraison;
    private BigDecimal montantCOD;
    private BigDecimal montantARecevoir;
    private String message;
}
```

### 📦 Interface API : `VendeurApi.java`
```java
// Fichier : target/generated-sources/openapi/sn/votreplateforme/logistique/api/VendeurApi.java
package sn.votreplateforme.logistique.api;

@Validated
@Tag(name = "Vendeur")
public interface VendeurApi {
    
    @Operation(summary = "Créer une nouvelle livraison")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Livraison créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @PostMapping(
        value = "/vendeur/livraisons",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LivraisonResponse> vendeurLivraisonsPost(
        @Valid @RequestBody CreateLivraisonRequest createLivraisonRequest
    );
    
    // ... autres méthodes ...
}
```

---

## 🏗️ Étape 3 : Créer l'entité JPA

**Fichier :** `src/main/java/sn/votreplateforme/logistique/entity/Livraison.java`

```java
package sn.votreplateforme.logistique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "livraisons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Livraison {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String numeroTracking;
    
    private String qrCodeUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Vendeur vendeur;
    
    // Informations client
    @Column(nullable = false)
    private String nomClient;
    
    @Column(nullable = false)
    private String telephoneClient;
    
    // Adresse
    @Embedded
    private Adresse adresseDestination;
    
    // Détails colis
    @Column(nullable = false)
    private String descriptionProduit;
    
    private Boolean fragile = false;
    
    private Double poids;
    
    // Financier
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantCOD;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fraisLivraison;
    
    // Statut
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLivraison statut = StatutLivraison.EN_ATTENTE_RAMASSAGE;
    
    // Dates
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;
    
    private LocalDateTime dateRamassage;
    
    private LocalDateTime dateLivraison;
    
    // Options
    @Enumerated(EnumType.STRING)
    private TypeUrgence urgence = TypeUrgence.NORMAL;
    
    private String creneauSouhaite;
    
    @Column(length = 500)
    private String notesPourLivreur;
    
    // Après livraison
    @Column(precision = 10, scale = 2)
    private BigDecimal cashCollecte;
    
    private String commentaireLivraison;
}
```

---

## 🗄️ Étape 4 : Créer le Repository

**Fichier :** `src/main/java/sn/votreplateforme/logistique/repository/LivraisonRepository.java`

```java
package sn.votreplateforme.logistique.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.votreplateforme.logistique.entity.Livraison;
import sn.votreplateforme.logistique.entity.StatutLivraison;
import sn.votreplateforme.logistique.entity.Vendeur;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long> {
    
    Optional<Livraison> findByNumeroTracking(String numeroTracking);
    
    List<Livraison> findByVendeur(Vendeur vendeur);
    
    List<Livraison> findByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut);
    
    List<Livraison> findByStatut(StatutLivraison statut);
    
    long countByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut);
}
```

---

## 💼 Étape 5 : Créer le Service

**Fichier :** `src/main/java/sn/votreplateforme/logistique/service/LivraisonService.java`

```java
package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.CreateLivraisonRequest;
import sn.votreplateforme.logistique.dto.LivraisonResponse;
import sn.votreplateforme.logistique.entity.*;
import sn.votreplateforme.logistique.repository.LivraisonRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;
import sn.votreplateforme.logistique.repository.ZoneRepository;
import sn.votreplateforme.logistique.security.SecurityUtils;
import sn.votreplateforme.logistique.util.TrackingNumberGenerator;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class LivraisonService {
    
    private final LivraisonRepository livraisonRepository;
    private final VendeurRepository vendeurRepository;
    private final ZoneRepository zoneRepository;
    private final QRCodeService qrCodeService;
    private final TarifService tarifService;
    private final TrackingNumberGenerator trackingNumberGenerator;
    
    @Transactional
    public LivraisonResponse creerLivraison(CreateLivraisonRequest request) {
        log.info("Création d'une nouvelle livraison pour le client: {}", request.getNomClient());
        
        // 1. Récupérer le vendeur connecté
        String telephone = SecurityUtils.getCurrentUserTelephone();
        Vendeur vendeur = vendeurRepository.findByTelephone(telephone)
            .orElseThrow(() -> new ResourceNotFoundException("Vendeur non trouvé"));
        
        // 2. Récupérer la zone
        Zone zone = zoneRepository.findById(request.getZoneId())
            .orElseThrow(() -> new ResourceNotFoundException("Zone non trouvée"));
        
        // 3. Calculer le tarif
        BigDecimal fraisLivraison = tarifService.calculerTarif(
            zone, 
            request.getUrgence(), 
            request.getPoids()
        );
        
        // 4. Générer le numéro de tracking
        String numeroTracking = trackingNumberGenerator.generate();
        
        // 5. Créer l'adresse
        Adresse adresse = Adresse.builder()
            .commune(request.getCommune())
            .quartier(request.getQuartier())
            .adresseComplete(request.getAdresseComplete())
            .pointRepere(request.getPointRepere())
            .zone(zone)
            .build();
        
        // 6. Créer la livraison
        Livraison livraison = Livraison.builder()
            .numeroTracking(numeroTracking)
            .vendeur(vendeur)
            .nomClient(request.getNomClient())
            .telephoneClient(request.getTelephoneClient())
            .adresseDestination(adresse)
            .descriptionProduit(request.getDescriptionProduit())
            .fragile(request.getFragile() != null ? request.getFragile() : false)
            .poids(request.getPoids())
            .montantCOD(request.getMontantCOD())
            .fraisLivraison(fraisLivraison)
            .statut(StatutLivraison.EN_ATTENTE_RAMASSAGE)
            .urgence(request.getUrgence() != null ? request.getUrgence() : TypeUrgence.NORMAL)
            .creneauSouhaite(request.getCreneauSouhaite())
            .notesPourLivreur(request.getNotesPourLivreur())
            .build();
        
        // 7. Générer le QR code
        String qrCodeUrl = qrCodeService.generateQRCodeUrl(numeroTracking);
        livraison.setQrCodeUrl(qrCodeUrl);
        
        // 8. Sauvegarder
        livraison = livraisonRepository.save(livraison);
        
        log.info("Livraison créée avec succès: {}", numeroTracking);
        
        // 9. Mapper vers DTO Response
        BigDecimal montantARecevoir = request.getMontantCOD().subtract(fraisLivraison);
        
        return LivraisonResponse.builder()
            .id(livraison.getId())
            .numeroTracking(numeroTracking)
            .qrCodeUrl(qrCodeUrl)
            .statut(StatutLivraison.EN_ATTENTE_RAMASSAGE)
            .dateCreation(livraison.getDateCreation())
            .fraisLivraison(fraisLivraison)
            .montantCOD(request.getMontantCOD())
            .montantARecevoir(montantARecevoir)
            .message("Livraison créée avec succès ! Écrivez #" + numeroTracking + " sur votre colis.")
            .build();
    }
}
```

---

## 🎮 Étape 6 : Implémenter le Controller

**Fichier :** `src/main/java/sn/votreplateforme/logistique/controller/VendeurController.java`

```java
package sn.votreplateforme.logistique.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.votreplateforme.logistique.api.VendeurApi; // Interface générée
import sn.votreplateforme.logistique.dto.CreateLivraisonRequest;
import sn.votreplateforme.logistique.dto.LivraisonResponse;
import sn.votreplateforme.logistique.service.LivraisonService;

/**
 * Controller Vendeur - Implémente l'interface VendeurApi générée par OpenAPI
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class VendeurController implements VendeurApi {
    
    private final LivraisonService livraisonService;
    
    /**
     * POST /api/vendeur/livraisons
     * Créer une nouvelle livraison
     */
    @Override
    public ResponseEntity<LivraisonResponse> vendeurLivraisonsPost(
        CreateLivraisonRequest createLivraisonRequest
    ) {
        log.info("Création d'une livraison pour: {}", createLivraisonRequest.getNomClient());
        
        LivraisonResponse response = livraisonService.creerLivraison(createLivraisonRequest);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
    // Les autres méthodes de VendeurApi à implémenter...
}
```

---

## ✅ Étape 7 : Tester avec Swagger UI

1. **Démarrer l'application**
```bash
mvn spring-boot:run
```

2. **Ouvrir Swagger UI**
```
http://localhost:8080/api/swagger-ui.html
```

3. **Tester l'endpoint**
- Aller dans la section "Vendeur"
- Cliquer sur `POST /vendeur/livraisons`
- Cliquer sur "Try it out"
- Remplir le JSON :

```json
{
  "nomClient": "Aïssatou Ndiaye",
  "telephoneClient": "772345678",
  "commune": "Dakar",
  "quartier": "Mermoz",
  "adresseComplete": "Cité Biagui, Villa 45",
  "pointRepere": "Face à la mosquée",
  "descriptionProduit": "Ensemble wax",
  "fragile": true,
  "poids": 1.5,
  "montantCOD": 35000,
  "zoneId": 2,
  "urgence": "NORMAL",
  "creneauSouhaite": "APRES_MIDI"
}
```

4. **Voir la réponse**
```json
{
  "id": 1,
  "numeroTracking": "DKR-00001",
  "qrCodeUrl": "https://track.votreplateforme.sn/DKR-00001/deliver",
  "statut": "EN_ATTENTE_RAMASSAGE",
  "dateCreation": "2025-11-17T14:30:00Z",
  "fraisLivraison": 1500,
  "montantCOD": 35000,
  "montantARecevoir": 33500,
  "message": "Livraison créée avec succès ! Écrivez #DKR-00001 sur votre colis."
}
```

---

## 📊 Récapitulatif du workflow

```
1. DÉFINIR dans openapi.yaml
   ↓
2. GÉNÉRER avec mvn generate-sources
   ↓
3. CRÉER Entity + Repository
   ↓
4. CRÉER Service (logique métier)
   ↓
5. IMPLÉMENTER Controller (interface générée)
   ↓
6. TESTER avec Swagger UI
```

---

## 🎯 Avantages de cette approche

✅ **Contrat d'API clair** - openapi.yaml = documentation
✅ **Validation automatique** - Annotations générées (@Valid, @NotBlank, etc.)
✅ **Pas de désynchronisation** - Swagger UI toujours à jour
✅ **Type-safe** - Interfaces générées = compilation fail si erreur
✅ **Gain de temps** - 30+ DTOs générés automatiquement
✅ **Swagger UI gratuit** - Documentation interactive

---

## 🚀 Prochains endpoints à implémenter

En suivant exactement le même workflow :

1. `GET /vendeur/dashboard` → VendeurService.getDashboard()
2. `GET /vendeur/livraisons` → LivraisonService.getMesLivraisons()
3. `GET /vendeur/finances` → FinanceService.getVendeurFinances()
4. `POST /vendeur/demande-paiement` → FinanceService.demanderPaiement()

**Et ainsi de suite pour tous les 40+ endpoints !**

---

**Bon développement ! 🚀**
