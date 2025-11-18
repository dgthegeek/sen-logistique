#!/bin/bash

echo "============================================"
echo "   🚚 Plateforme Logistique Dakar"
echo "   Script de démarrage"
echo "============================================"
echo ""

# Couleurs
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Vérifier Java
echo -e "${YELLOW}Vérification de Java...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java n'est pas installé${NC}"
    echo "Installez Java 17+ : https://adoptium.net/"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java 17+ requis (version détectée: $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java $JAVA_VERSION détecté${NC}"
echo ""

# Vérifier Maven
echo -e "${YELLOW}Vérification de Maven...${NC}"
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven n'est pas installé${NC}"
    echo "Installez Maven : https://maven.apache.org/install.html"
    exit 1
fi
echo -e "${GREEN}✅ Maven détecté${NC}"
echo ""

# Vérifier PostgreSQL
echo -e "${YELLOW}Vérification de PostgreSQL...${NC}"
if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}⚠️  PostgreSQL CLI non détecté${NC}"
    echo "Assurez-vous que PostgreSQL est installé et en cours d'exécution"
    echo ""
fi

# Étape 1 : Générer les sources depuis OpenAPI
echo -e "${YELLOW}📦 Étape 1/3 : Génération des sources depuis OpenAPI...${NC}"
mvn generate-sources
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Erreur lors de la génération des sources${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Sources générées avec succès${NC}"
echo ""

# Étape 2 : Compilation
echo -e "${YELLOW}🔨 Étape 2/3 : Compilation du projet...${NC}"
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Erreur lors de la compilation${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Projet compilé avec succès${NC}"
echo ""

# Étape 3 : Démarrage
echo -e "${YELLOW}🚀 Étape 3/3 : Démarrage de l'application...${NC}"
echo ""
echo -e "${GREEN}L'application démarrera sur : http://localhost:8080/api${NC}"
echo -e "${GREEN}Swagger UI : http://localhost:8080/api/swagger-ui.html${NC}"
echo ""
echo -e "${YELLOW}Appuyez sur Ctrl+C pour arrêter l'application${NC}"
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=dev
