#!/bin/bash
# ============================================
# SCRIPT HEALTH CHECK
# Vérifie que l'application fonctionne
# ============================================
set -e

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🔍 Vérification de la santé de l'application...${NC}"

# ==========================================
# 1. Vérifier que les containers tournent
# ==========================================
echo -e "${YELLOW}1️⃣ Vérification des containers...${NC}"

APP_STATUS=$(docker inspect -f '{{.State.Status}}' sen-logistique-app 2>/dev/null || echo "not found")
DB_STATUS=$(docker inspect -f '{{.State.Status}}' sen-logistique-db 2>/dev/null || echo "not found")

if [ "$APP_STATUS" != "running" ]; then
    echo -e "${RED}❌ Container app n'est pas en cours d'exécution${NC}"
    exit 1
fi

if [ "$DB_STATUS" != "running" ]; then
    echo -e "${RED}❌ Container PostgreSQL n'est pas en cours d'exécution${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Containers en cours d'exécution${NC}"

# ==========================================
# 2. Vérifier la santé de PostgreSQL
# ==========================================
echo -e "${YELLOW}2️⃣ Vérification de PostgreSQL...${NC}"

DB_HEALTH=$(docker inspect -f '{{.State.Health.Status}}' sen-logistique-db 2>/dev/null || echo "unknown")

if [ "$DB_HEALTH" != "healthy" ] && [ "$DB_HEALTH" != "unknown" ]; then
    echo -e "${RED}❌ PostgreSQL n'est pas healthy${NC}"
    exit 1
fi

echo -e "${GREEN}✅ PostgreSQL fonctionne${NC}"

# ==========================================
# 3. Attendre que l'app soit prête (max 60s)
# ==========================================
echo -e "${YELLOW}3️⃣ Attente du démarrage de l'application...${NC}"

MAX_ATTEMPTS=12
ATTEMPT=0
WAIT_TIME=5

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    echo -e "${YELLOW}   Tentative $ATTEMPT/$MAX_ATTEMPTS...${NC}"
    
    # Vérifier si l'app répond
    if docker exec sen-logistique-app wget --quiet --tries=1 --spider http://localhost:8080/api/zones 2>/dev/null; then
        echo -e "${GREEN}✅ Application répond !${NC}"
        break
    fi
    
    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        echo -e "${RED}❌ Timeout: L'application ne répond pas après 60 secondes${NC}"
        echo -e "${RED}📋 Logs de l'application:${NC}"
        docker logs --tail=50 sen-logistique-app
        exit 1
    fi
    
    sleep $WAIT_TIME
done

# ==========================================
# 4. Vérifier un endpoint public
# ==========================================
echo -e "${YELLOW}4️⃣ Test de l'endpoint /api/zones...${NC}"

HTTP_CODE=$(docker exec sen-logistique-app wget --server-response --spider --quiet http://localhost:8080/api/zones 2>&1 | grep -oP 'HTTP/[0-9.]+ \K[0-9]+' | head -1 || echo "000")

if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${RED}❌ L'endpoint /api/zones ne répond pas correctement (HTTP $HTTP_CODE)${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Endpoint /api/zones OK${NC}"

# ==========================================
# 5. Résumé
# ==========================================
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ HEALTH CHECK RÉUSSI${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🐳 Containers: Running${NC}"
echo -e "${GREEN}🗄️  PostgreSQL: Healthy${NC}"
echo -e "${GREEN}🌐 API: Responsive${NC}"
echo -e "${GREEN}========================================${NC}"

exit 0
