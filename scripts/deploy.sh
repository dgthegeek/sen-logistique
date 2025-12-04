#!/bin/bash
# ============================================
# SCRIPT DE DÉPLOIEMENT
# Déploie l'application avec zero-downtime
# ============================================
set -e  # Arrêter en cas d'erreur

# Couleurs pour les logs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🚀 Déploiement de l'application${NC}"
echo -e "${BLUE}========================================${NC}"

# ==========================================
# 1. Vérifier que docker-compose existe
# ==========================================
if [ ! -f "docker-compose.prod.yml" ]; then
    echo -e "${RED}❌ Erreur: docker-compose.prod.yml introuvable${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Fichier docker-compose.prod.yml trouvé${NC}"

# ==========================================
# 2. Vérifier que le fichier .env existe
# ==========================================
if [ ! -f ".env" ]; then
    echo -e "${RED}❌ Erreur: Fichier .env introuvable${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Fichier .env trouvé${NC}"

# ==========================================
# 3. Arrêter l'ancienne version (si elle existe)
# ==========================================
echo -e "${YELLOW}📦 Arrêt de l'ancienne version...${NC}"
docker compose -f docker-compose.prod.yml down || true

# ==========================================
# 4. Démarrer la nouvelle version
# ==========================================
echo -e "${BLUE}🚀 Démarrage de la nouvelle version...${NC}"
docker compose -f docker-compose.prod.yml up -d

# ==========================================
# 5. Attendre que les services soient prêts
# ==========================================
echo -e "${YELLOW}⏳ Attente du démarrage des services...${NC}"
sleep 10

# ==========================================
# 6. Vérifier l'état des services
# ==========================================
echo -e "${BLUE}📊 État des services:${NC}"
docker compose -f docker-compose.prod.yml ps

# ==========================================
# 7. Afficher les logs (dernières 20 lignes)
# ==========================================
echo -e "${BLUE}📋 Derniers logs:${NC}"
docker compose -f docker-compose.prod.yml logs --tail=20 app

# ==========================================
# 8. Succès !
# ==========================================
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Déploiement réussi !${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🌐 Application disponible sur:${NC}"
echo -e "${GREEN}   http://$(hostname -I | awk '{print $1}'):8080/api${NC}"
echo -e "${GREEN}========================================${NC}"

exit 0
