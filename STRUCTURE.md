# 📁 Structure du Projet - Plateforme Logistique Dakar

## Vue d'ensemble

```
logistique-dakar/
│
├── 📄 pom.xml                          # Configuration Maven + Plugin OpenAPI Generator
├── 📄 README.md                        # Documentation principale
├── 📄 .gitignore                       # Fichiers à ignorer par Git
├── 📄 start.sh                         # Script de démarrage rapide
│
├── 📂 src/
│   ├── 📂 main/
│   │   ├── 📂 java/sn/votreplateforme/logistique/
│   │   │   │
│   │   │   ├── 📄 LogistiqueDakarApplication.java   # Classe principale Spring Boot
│   │   │   │
│   │   │   ├── 📂 config/                  # ⚙️ Configuration Spring
│   │   │   │   ├── SecurityConfig.java     # Spring Security + JWT
│   │   │   │   ├── CorsConfig.java         # Configuration CORS
│   │   │   │   ├── OpenApiConfig.java      # Configuration Swagger UI
│   │   │   │   └── TwilioConfig.java       # Configuration Twilio/WhatsApp
│   │   │   │
│   │   │   ├── 📂 controller/              # 🎮 Controllers (implémentent les interfaces générées)
│   │   │   │   ├── AuthController.java     # Implémente AuthenticationApi
│   │   │   │   ├── VendeurController.java  # Implémente VendeurApi
│   │   │   │   ├── AdminRamassageController.java
│   │   │   │   ├── AdminLivraisonController.java
│   │   │   │   ├── AdminFinanceController.java
│   │   │   │   ├── DeliveryController.java
│   │   │   │   ├── TrackingController.java
│   │   │   │   └── ZoneController.java
│   │   │   │
│   │   │   ├── 📂 service/                 # 💼 Services métier
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── VendeurService.java
│   │   │   │   ├── LivraisonService.java
│   │   │   │   ├── RamassageService.java
│   │   │   │   ├── FinanceService.java
│   │   │   │   ├── NotificationService.java  # Gestion WhatsApp
│   │   │   │   ├── QRCodeService.java        # Génération QR codes
│   │   │   │   ├── TrackingService.java
│   │   │   │   └── ZoneService.java
│   │   │   │
│   │   │   ├── 📂 repository/              # 🗄️ Repositories JPA
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── VendeurRepository.java
│   │   │   │   ├── LivraisonRepository.java
│   │   │   │   ├── ZoneRepository.java
│   │   │   │   ├── QuartierRepository.java
│   │   │   │   └── TransactionRepository.java
│   │   │   │
│   │   │   ├── 📂 entity/                  # 🏗️ Entités JPA (base de données)
│   │   │   │   ├── User.java               # Classe abstraite
│   │   │   │   ├── Vendeur.java            # extends User
│   │   │   │   ├── Admin.java              # extends User
│   │   │   │   ├── Livraison.java
│   │   │   │   ├── Adresse.java            # @Embeddable
│   │   │   │   ├── Zone.java
│   │   │   │   ├── Quartier.java
│   │   │   │   └── Transaction.java
│   │   │   │
│   │   │   ├── 📂 mapper/                  # 🔄 Mappers DTO <-> Entity
│   │   │   │   ├── VendeurMapper.java
│   │   │   │   ├── LivraisonMapper.java
│   │   │   │   ├── ZoneMapper.java
│   │   │   │   └── TransactionMapper.java
│   │   │   │
│   │   │   ├── 📂 security/                # 🔒 Sécurité JWT
│   │   │   │   ├── JwtTokenProvider.java   # Génération/validation JWT
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── UserDetailsServiceImpl.java
│   │   │   │   └── SecurityUtils.java
│   │   │   │
│   │   │   ├── 📂 exception/               # ⚠️ Gestion des exceptions
│   │   │   │   ├── GlobalExceptionHandler.java  # @ControllerAdvice
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── UnauthorizedException.java
│   │   │   │
│   │   │   └── 📂 util/                    # 🛠️ Classes utilitaires
│   │   │       ├── TrackingNumberGenerator.java
│   │   │       ├── TarifCalculator.java
│   │   │       └── DateUtils.java
│   │   │
│   │   └── 📂 resources/
│   │       ├── 📄 openapi.yaml             # 🔑 Spécification API (source de vérité)
│   │       ├── 📄 application.yml          # Configuration Spring Boot
│   │       └── 📂 db/migration/            # 📊 Scripts Flyway
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_zones_table.sql
│   │           ├── V3__create_livraisons_table.sql
│   │           └── V4__create_transactions_table.sql
│   │
│   └── 📂 test/
│       └── 📂 java/sn/votreplateforme/logistique/
│           ├── controller/                 # Tests des controllers
│           ├── service/                    # Tests des services
│           └── integration/                # Tests d'intégration
│
└── 📂 target/                              # 🎯 Généré par Maven (à ignorer par Git)
    ├── 📂 classes/                         # Classes compilées
    ├── 📂 generated-sources/
    │   └── 📂 openapi/                     # 🤖 CODE GÉNÉRÉ AUTOMATIQUEMENT
    │       ├── 📂 sn/votreplateforme/logistique/
    │       │   ├── 📂 dto/                 # DTOs générés depuis openapi.yaml
    │       │   │   ├── CreateLivraisonRequest.java
    │       │   │   ├── LivraisonResponse.java
    │       │   │   ├── VendeurDashboard.java
    │       │   │   ├── LoginRequest.java
    │       │   │   ├── RegisterRequest.java
    │       │   │   └── ... (30+ DTOs)
    │       │   │
    │       │   └── 📂 api/                 # Interfaces API générées
    │       │       ├── AuthenticationApi.java
    │       │       ├── VendeurApi.java
    │       │       ├── AdminRamassagesApi.java
    │       │       ├── AdminLivraisonsApi.java
    │       │       ├── AdminFinancesApi.java
    │       │       ├── DeliveryApi.java
    │       │       ├── TrackingApi.java
    │       │       └── ZonesApi.java
    │       │
    └── logistique-dakar-1.0.0-SNAPSHOT.jar # JAR exécutable
```

---

## 🔄 Workflow de développement

### 1️⃣ Modifier l'API

```bash
# 1. Éditer le fichier OpenAPI
vim src/main/resources/openapi.yaml

# 2. Régénérer les interfaces et DTOs
mvn generate-sources

# 3. Implémenter les nouvelles interfaces dans les controllers
```

### 2️⃣ Créer une nouvelle migration Flyway

```bash
# Créer un nouveau fichier
touch src/main/resources/db/migration/V5__add_column_xyz.sql

# Flyway appliquera automatiquement au démarrage
```

### 3️⃣ Ajouter un nouvel endpoint

1. **Ajouter dans `openapi.yaml`** :
```yaml
/admin/stats:
  get:
    tags:
      - Admin
    summary: Statistiques globales
    responses:
      '200':
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdminStats'
```

2. **Régénérer** : `mvn generate-sources`

3. **Implémenter** :
```java
@RestController
public class AdminController implements AdminApi {
    // La méthode adminStatsGet() est générée automatiquement dans AdminApi
    @Override
    public ResponseEntity<AdminStats> adminStatsGet() {
        // Votre implémentation
    }
}
```

---

## 📦 Packages importants

### `sn.votreplateforme.logistique.dto` (généré)
- Tous les DTOs Request/Response
- Générés automatiquement depuis `openapi.yaml`
- **NE PAS MODIFIER** directement
- Avec annotations Lombok (@Data, @Builder, etc.)

### `sn.votreplateforme.logistique.api` (généré)
- Interfaces des controllers
- Générées automatiquement depuis `openapi.yaml`
- **NE PAS MODIFIER** directement
- À implémenter dans vos controllers

### `sn.votreplateforme.logistique.entity`
- Entités JPA (base de données)
- Créées manuellement
- Représentent les tables

### `sn.votreplateforme.logistique.mapper`
- Conversion DTO ↔ Entity
- Créés manuellement
- Utilisent MapStruct ou manuellement

---

## 🎯 Ordre de développement recommandé

### Phase 1 : Foundation (Jour 1-2)
1. ✅ Setup projet + OpenAPI
2. 🔄 Créer les entités JPA
3. 🔄 Créer les repositories
4. 🔄 Migrations Flyway initiales
5. 🔄 Configuration Security + JWT

### Phase 2 : Authentication (Jour 2-3)
6. 🔄 Implémenter `AuthController`
7. 🔄 Service `AuthService`
8. 🔄 Tests authentification

### Phase 3 : Vendeur (Jour 3-5)
9. 🔄 Implémenter `VendeurController`
10. 🔄 Service `LivraisonService`
11. 🔄 Service `ZoneService`
12. 🔄 Calcul tarifs

### Phase 4 : Admin (Jour 5-8)
13. 🔄 Ramassages groupés
14. 🔄 Service QR Code
15. 🔄 Confirmation livraison
16. 🔄 Finances

### Phase 5 : Notifications (Jour 8-10)
17. 🔄 Service WhatsApp (Twilio)
18. 🔄 Intégration notifications

### Phase 6 : Polish (Jour 10-12)
19. 🔄 Tests
20. 🔄 Documentation
21. 🔄 Déploiement

---

## 🛠️ Commandes Maven utiles

```bash
# Générer uniquement les sources OpenAPI
mvn generate-sources

# Compiler sans tests
mvn clean install -DskipTests

# Lancer l'application
mvn spring-boot:run

# Lancer en mode dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Créer le JAR
mvn clean package

# Voir les dépendances
mvn dependency:tree

# Nettoyer
mvn clean
```

---

## 📊 Base de données - Tables principales

```
users (abstract)
├── vendeurs (extends users)
└── admins (extends users)

livraisons
├── FK: vendeur_id
└── FK: zone_id

zones
└── quartiers
    └── FK: zone_id

transactions
└── FK: vendeur_id
```

---

## 🔐 Sécurité

### Endpoints publics (pas d'auth)
- `/api/auth/login`
- `/api/auth/register`
- `/api/tracking/{numero}`
- `/api/delivery/{numero}` (scan QR)
- `/api/zones`
- `/api/quartiers/**`

### Endpoints protégés (JWT requis)
- `/api/vendeur/**` → Role: VENDEUR
- `/api/admin/**` → Role: ADMIN

---

## 📝 Conventions de code

### Nommage
- **Entities** : `Livraison.java`, `User.java`
- **DTOs** : Générés automatiquement depuis OpenAPI
- **Services** : `LivraisonService.java`
- **Controllers** : `VendeurController.java` (implémente `VendeurApi`)
- **Repositories** : `LivraisonRepository.java`

### Packages
- `entity` : Entités JPA
- `dto` : DTOs générés (ne pas créer manuellement)
- `api` : Interfaces générées (ne pas créer manuellement)
- `controller` : Implémentations des interfaces API
- `service` : Logique métier
- `repository` : Accès données

---

## ✨ Points clés

1. **OpenAPI = Source de vérité** : Toute modification d'API commence par `openapi.yaml`
2. **Génération automatique** : DTOs et interfaces générés via Maven
3. **Ne pas modifier le code généré** : Il sera écrasé à chaque build
4. **Implémenter les interfaces** : Vos controllers implémentent les interfaces générées
5. **Flyway** : Toute modification DB passe par une migration
6. **Tests** : Tester chaque service et controller

---

**🚀 Prêt à démarrer le développement !**
