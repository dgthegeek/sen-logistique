# 🚚 Plateforme Logistique Dakar

Plateforme de gestion logistique pour petits vendeurs Instagram/Facebook à Dakar.

## 📋 Prérequis

- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Node.js 18+ (pour le frontend Angular)

## 🚀 Démarrage Rapide

### 1. Cloner le projet

```bash
git clone <votre-repo>
cd logistique-dakar
```

### 2. Configuration Base de données

Créer la base de données PostgreSQL :

```sql
CREATE DATABASE logistique_dakar;
CREATE USER logistique_user WITH PASSWORD 'votre_password';
GRANT ALL PRIVILEGES ON DATABASE logistique_dakar TO logistique_user;
```

### 3. Configuration de l'application

Créer un fichier `src/main/resources/application-local.yml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/logistique_dakar
    username: logistique_user
    password: votre_password

jwt:
  secret: votre_secret_jwt_tres_long_et_complexe

twilio:
  account-sid: votre_account_sid
  auth-token: votre_auth_token
  whatsapp-from: whatsapp:+14155238886
  enabled: true
```

### 4. Générer les interfaces et DTOs depuis OpenAPI

```bash
mvn clean generate-sources
```

Cette commande va :
- Lire le fichier `src/main/resources/openapi.yaml`
- Générer tous les DTOs dans `target/generated-sources/openapi/sn/votreplateforme/logistique/dto/`
- Générer toutes les interfaces API dans `target/generated-sources/openapi/sn/votreplateforme/logistique/api/`

### 5. Compiler le projet

```bash
mvn clean install
```

### 6. Lancer l'application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

L'application démarre sur `http://localhost:8080/api`

### 7. Accéder à Swagger UI

Une fois l'application lancée, accéder à :

```
http://localhost:8080/api/swagger-ui.html
```

## 📁 Structure du projet

```
logistique-dakar/
├── src/
│   ├── main/
│   │   ├── java/sn/votreplateforme/logistique/
│   │   │   ├── LogistiqueDakarApplication.java
│   │   │   ├── config/           # Configuration (Security, CORS, etc.)
│   │   │   ├── controller/       # Implémentation des interfaces API générées
│   │   │   ├── service/          # Services métier
│   │   │   ├── repository/       # Repositories JPA
│   │   │   ├── entity/           # Entités JPA
│   │   │   ├── mapper/           # Mappers DTO <-> Entity
│   │   │   ├── security/         # JWT, Authentication
│   │   │   ├── exception/        # Gestion des exceptions
│   │   │   └── util/             # Classes utilitaires
│   │   └── resources/
│   │       ├── openapi.yaml      # Spécification OpenAPI
│   │       ├── application.yml   # Configuration
│   │       └── db/migration/     # Scripts Flyway
│   └── test/                     # Tests unitaires et d'intégration
├── target/
│   └── generated-sources/
│       └── openapi/              # Code généré automatiquement
│           ├── dto/              # DTOs générés
│           └── api/              # Interfaces API générées
├── pom.xml
└── README.md
```

## 🔧 Workflow de développement

### Modifier l'API

1. Modifier le fichier `src/main/resources/openapi.yaml`
2. Régénérer le code : `mvn generate-sources`
3. Implémenter/adapter les controllers

### Exemple d'implémentation d'un controller

Les interfaces sont générées automatiquement. Vous devez les implémenter :

```java
// Interface générée automatiquement dans target/generated-sources/
package sn.votreplateforme.logistique.api;

public interface VendeurApi {
    ResponseEntity<VendeurDashboard> vendeurDashboardGet();
    // ... autres méthodes
}

// Votre implémentation
package sn.votreplateforme.logistique.controller;

@RestController
@RequiredArgsConstructor
public class VendeurController implements VendeurApi {
    
    private final VendeurService vendeurService;
    
    @Override
    public ResponseEntity<VendeurDashboard> vendeurDashboardGet() {
        VendeurDashboard dashboard = vendeurService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }
}
```

## 🗄️ Migrations Base de données (Flyway)

Les migrations sont dans `src/main/resources/db/migration/`

Nommage : `V{version}__{description}.sql`

Exemple : `V1__create_users_table.sql`

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20) UNIQUE NOT NULL,
    -- ...
);
```

## 🧪 Tests

```bash
# Tous les tests
mvn test

# Tests d'un package spécifique
mvn test -Dtest=VendeurControllerTest
```

## 📦 Build pour production

```bash
mvn clean package -Pprod
```

Le JAR sera dans `target/logistique-dakar-1.0.0-SNAPSHOT.jar`

## 🚀 Déploiement

```bash
# Avec profil production
java -jar target/logistique-dakar-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🔑 Variables d'environnement (Production)

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-host:5432/logistique_dakar
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=prod_password
export JWT_SECRET=votre_secret_production_tres_long
export TWILIO_ACCOUNT_SID=votre_account_sid
export TWILIO_AUTH_TOKEN=votre_auth_token
export TWILIO_WHATSAPP_FROM=whatsapp:+votrenumero
export APP_BASE_URL=https://api.votreplateforme.sn
export TRACKING_URL=https://track.votreplateforme.sn
```

## 📚 Documentation API

- **Swagger UI** : http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON** : http://localhost:8080/api/v3/api-docs
- **Fichier source** : `src/main/resources/openapi.yaml`

## 🛠️ Commandes utiles

```bash
# Nettoyer et compiler
mvn clean install

# Regénérer les sources depuis OpenAPI
mvn generate-sources

# Lancer en mode dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Lancer avec debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Build sans tests
mvn clean package -DskipTests

# Voir les dépendances
mvn dependency:tree
```

## 📝 Prochaines étapes du développement

1. ✅ Setup projet + Swagger
2. 🔄 Créer les entités JPA (User, Vendeur, Livraison, Zone, etc.)
3. 🔄 Repositories JPA
4. 🔄 Configuration Security + JWT
5. 🔄 Implémenter Authentication endpoints
6. 🔄 Implémenter Vendeur endpoints
7. 🔄 Implémenter Admin endpoints
8. 🔄 Service Notifications WhatsApp
9. 🔄 Service QR Code
10. 🔄 Tests

## 🤝 Contribution

1. Fork le projet
2. Créer une branche (`git checkout -b feature/AmazingFeature`)
3. Commit (`git commit -m 'Add AmazingFeature'`)
4. Push (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 Licence

Propriétaire - Plateforme Logistique Dakar

## 📧 Contact

Email : contact@votreplateforme.sn
