# ============================================
# DOCKERFILE - Spring Boot Application
# Application: Plateforme Logistique Dakar
# Java Version: 21
# Build Strategy: Multi-stage (optimisé)
# ============================================

# ==========================================
# STAGE 1 : BUILD (Construction de l'app)
# ==========================================
# On utilise Maven + Java 21 pour compiler l'application

FROM maven:3.9-eclipse-temurin-21 AS build

# Définir le répertoire de travail dans le container
WORKDIR /app

# ASTUCE : On copie d'abord pom.xml seul
# Pourquoi ? Docker met en cache les layers
# Si pom.xml ne change pas, les dépendances ne sont pas re-téléchargées
COPY pom.xml .

# Télécharger les dépendances Maven (mis en cache si pom.xml inchangé)
RUN mvn dependency:go-offline -B

# Maintenant on copie tout le code source
COPY src ./src

# IMPORTANT : Copier aussi le fichier openapi.yaml pour la génération
COPY src/main/resources/openapi.yaml ./src/main/resources/

# Compiler l'application (génération DTOs + compilation + packaging)
# -DskipTests : On skip les tests car ils seront exécutés par GitHub Actions
# Le JAR sera créé dans /app/target/
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2 : RUNTIME (Image finale légère)
# ==========================================
# On utilise seulement le JRE (pas Maven, plus léger)

FROM eclipse-temurin:21-jre-alpine

# Informations sur l'image (métadonnées)
LABEL maintainer="dame@votreplateforme.sn"
LABEL version="1.0"
LABEL description="API Plateforme Logistique Dakar"

# Créer un utilisateur non-root pour la sécurité
# Pourquoi ? Ne JAMAIS exécuter une app en tant que root dans un container
RUN addgroup -S spring && adduser -S spring -G spring

# Créer les dossiers nécessaires
RUN mkdir -p /app/logs && \
    chown -R spring:spring /app

# Définir le répertoire de travail
WORKDIR /app

# Copier le JAR depuis le stage BUILD
# On renomme en app.jar pour simplifier
COPY --from=build /app/target/*.jar app.jar

# Changer l'ownership du JAR
RUN chown spring:spring app.jar

# Passer à l'utilisateur spring (sécurité)
USER spring

# Exposer le port 8080 (port par défaut de Spring Boot)
EXPOSE 8080

# Variables d'environnement par défaut
# Ces valeurs seront surchargées par docker-compose ou les secrets
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms128m -Xmx256m"


# Health check : Docker vérifie toutes les 30s que l'app est vivante
# Timeout de 3s, 3 tentatives avant de considérer l'app comme "unhealthy"
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# Commande pour démarrer l'application
# $JAVA_OPTS permet d'ajuster la mémoire JVM
# -Djava.security.egd=file:/dev/./urandom : Accélère le démarrage
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

# ==========================================
# NOTES IMPORTANTES
# ==========================================
# 
# 1. Multi-stage build :
#    - Stage 1 (build) : ~800 MB (Maven + code source)
#    - Stage 2 (runtime) : ~200 MB (seulement JRE + JAR)
#    → On économise 600 MB !
#
# 2. Layers Docker (cache) :
#    - pom.xml copié en premier
#    - Si pom.xml inchangé → dépendances en cache
#    - Build beaucoup plus rapide !
#
# 3. Sécurité :
#    - Utilisateur non-root (spring)
#    - Image Alpine (moins de vulnérabilités)
#    - Health check intégré
#
# 4. Performance :
#    - JVM optimisée (-Xms512m -Xmx1024m)
#    - Random number generation rapide
#
# ==========================================
# COMMANDES UTILES
# ==========================================
#
# Build l'image :
# docker build -t sen-logistique:latest .
#
# Run le container localement :
# docker run -p 8080:8080 sen-logistique:latest
#
# Voir les logs :
# docker logs <container_id>
#
# Entrer dans le container :
# docker exec -it <container_id> sh
#
# ==========================================
