# 🚀 Guide de Démarrage Rapide - 15 minutes

## ✅ Prérequis installés

- [ ] Java 17+
- [ ] Maven 3.8+
- [ ] PostgreSQL 15+
- [ ] IDE (IntelliJ IDEA, VS Code, Eclipse)

---

## 📥 Étape 1 : Setup du projet (2 min)

```bash
# 1. Extraire le projet
cd logistique-dakar

# 2. Ouvrir dans votre IDE
# IntelliJ : File > Open > Sélectionner le dossier
# VS Code : code .
```

---

## 🗄️ Étape 2 : Créer la base de données (3 min)

```bash
# Se connecter à PostgreSQL
psql -U postgres

# Créer la base de données
CREATE DATABASE logistique_dakar;

# Créer un utilisateur (optionnel)
CREATE USER logistique_user WITH PASSWORD 'logistique2024';
GRANT ALL PRIVILEGES ON DATABASE logistique_dakar TO logistique_user;

# Quitter
\q
```

---

## ⚙️ Étape 3 : Configuration (2 min)

Créer le fichier `src/main/resources/application-local.yml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/logistique_dakar
    username: postgres
    password: votre_mot_de_passe

jwt:
  secret: changez_ce_secret_en_production_avec_une_longue_chaine_aleatoire

twilio:
  enabled: false  # Désactivé pour le dev local
```

---

## 🔨 Étape 4 : Build du projet (5 min)

```bash
# Générer les sources depuis OpenAPI
mvn generate-sources

# Compiler
mvn clean install -DskipTests
```

**Ce qui se passe :**
- Maven lit `src/main/resources/openapi.yaml`
- Génère automatiquement 30+ DTOs dans `target/generated-sources/openapi/dto/`
- Génère les interfaces API dans `target/generated-sources/openapi/api/`

---

## 🚀 Étape 5 : Démarrer l'application (1 min)

### Option A : Avec le script
```bash
./start.sh
```

### Option B : Avec Maven
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Option C : Depuis l'IDE
Exécuter la classe `LogistiqueDakarApplication.java`

---

## ✅ Étape 6 : Vérifier que ça marche (2 min)

### 1. Vérifier le démarrage
L'application démarre sur : `http://localhost:8080/api`

Vous devriez voir dans les logs :
```
Started LogistiqueDakarApplication in X seconds
```

### 2. Accéder à Swagger UI
Ouvrir dans le navigateur :
```
http://localhost:8080/api/swagger-ui.html
```

Vous verrez tous les endpoints organisés par tags :
- Authentication
- Vendeur
- Admin - Ramassages
- Admin - Livraisons
- Admin - Finances
- Delivery
- Tracking
- Zones

### 3. Tester un endpoint public
```bash
# Récupérer la liste des zones
curl http://localhost:8080/api/zones
```

---

## 🎯 Prochaines étapes

### 1. Créer les entités JPA

Créer dans `src/main/java/sn/votreplateforme/logistique/entity/` :

**User.java** (classe abstraite)
```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter
public abstract class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nom;
    private String prenom;
    
    @Column(unique = true)
    private String telephone;
    
    private String password;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    private boolean actif = true;
    
    @CreatedDate
    private LocalDateTime dateInscription;
}
```

**Vendeur.java**
```java
@Entity
@Getter @Setter
public class Vendeur extends User {
    private String nomBoutique;
    private String categorieActivite;
    
    // Adresse ramassage
    private String commune;
    private String quartier;
    private String adresseComplete;
    
    // Finances
    @Column(precision = 10, scale = 2)
    private BigDecimal soldeEnAttente = BigDecimal.ZERO;
    
    @OneToMany(mappedBy = "vendeur")
    private List<Livraison> livraisons;
}
```

**Livraison.java**
```java
@Entity
@Getter @Setter
public class Livraison {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String numeroTracking;
    
    private String qrCodeUrl;
    
    @ManyToOne
    @JoinColumn(name = "vendeur_id")
    private Vendeur vendeur;
    
    // Client
    private String nomClient;
    private String telephoneClient;
    
    // Adresse
    @Embedded
    private Adresse adresseDestination;
    
    // Colis
    private String descriptionProduit;
    private boolean fragile;
    private Double poids;
    
    // Financier
    @Column(precision = 10, scale = 2)
    private BigDecimal montantCOD;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal fraisLivraison;
    
    // Statut
    @Enumerated(EnumType.STRING)
    private StatutLivraison statut = StatutLivraison.EN_ATTENTE_RAMASSAGE;
    
    @CreatedDate
    private LocalDateTime dateCreation;
    
    private LocalDateTime dateRamassage;
    private LocalDateTime dateLivraison;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal cashCollecte;
    
    private String commentaireLivraison;
}
```

### 2. Créer les repositories

```java
public interface VendeurRepository extends JpaRepository<Vendeur, Long> {
    Optional<Vendeur> findByTelephone(String telephone);
}

public interface LivraisonRepository extends JpaRepository<Livraison, Long> {
    Optional<Livraison> findByNumeroTracking(String numeroTracking);
    List<Livraison> findByVendeurAndStatut(Vendeur vendeur, StatutLivraison statut);
}
```

### 3. Créer les migrations Flyway

`src/main/resources/db/migration/V1__create_users_table.sql` :
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    actif BOOLEAN DEFAULT true,
    date_inscription TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vendeurs (
    id BIGINT PRIMARY KEY REFERENCES users(id),
    nom_boutique VARCHAR(200),
    categorie_activite VARCHAR(100),
    commune VARCHAR(100),
    quartier VARCHAR(100),
    adresse_complete TEXT,
    solde_en_attente DECIMAL(10,2) DEFAULT 0
);
```

### 4. Implémenter AuthController

```java
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {
    
    private final AuthService authService;
    
    @Override
    public ResponseEntity<AuthResponse> authLoginPost(LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @Override
    public ResponseEntity<AuthResponse> authRegisterPost(RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

## 🐛 Troubleshooting

### Erreur "Port 8080 already in use"
```bash
# Trouver le processus
lsof -i :8080

# Tuer le processus
kill -9 <PID>
```

### Erreur de connexion PostgreSQL
```bash
# Vérifier que PostgreSQL tourne
sudo systemctl status postgresql

# Démarrer PostgreSQL
sudo systemctl start postgresql
```

### Erreur "Java version"
```bash
# Vérifier la version
java -version

# Installer Java 17
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17
```

### Les sources ne se génèrent pas
```bash
# Nettoyer et régénérer
mvn clean
mvn generate-sources -X
```

---

## 📚 Ressources

- **Documentation Swagger** : http://localhost:8080/api/swagger-ui.html
- **Fichier OpenAPI** : `src/main/resources/openapi.yaml`
- **Structure complète** : Voir `STRUCTURE.md`
- **Documentation principale** : Voir `README.md`

---

## 🎯 Objectif des 15 jours

| Jour | Phase | Objectif |
|------|-------|----------|
| 1-3  | Setup & Auth | Entités, Repositories, JWT, Login/Register |
| 4-6  | Vendeur | Créer livraisons, Dashboard, Finances |
| 7-10 | Admin | Ramassages, QR codes, Livraisons |
| 11-13| Notifications | WhatsApp, Gestion finances |
| 14-15| Polish | Tests, Documentation, Déploiement |

---

**🚀 C'est parti ! Bon développement !**
