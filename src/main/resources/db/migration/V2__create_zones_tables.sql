-- V2__create_zones_tables.sql
-- Création des tables pour les zones de livraison et quartiers

-- ==================== TABLE ZONES ====================
CREATE TABLE zones (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    tarif_standard NUMERIC(10, 2) NOT NULL,
    tarif_express NUMERIC(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Index sur le nom
CREATE INDEX idx_zones_nom ON zones(nom);

-- Index sur active pour filtrer les zones actives
CREATE INDEX idx_zones_active ON zones(active);

-- ==================== TABLE QUARTIERS ====================
CREATE TABLE quartiers (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    commune VARCHAR(100) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    zone_id BIGINT NOT NULL REFERENCES zones(id) ON DELETE RESTRICT
);

-- Index sur la commune pour l'auto-complétion
CREATE INDEX idx_quartiers_commune ON quartiers(commune);

-- Index sur le nom pour la recherche
CREATE INDEX idx_quartiers_nom ON quartiers(nom);

-- Index composite sur nom + commune (souvent recherchés ensemble)
CREATE INDEX idx_quartiers_nom_commune ON quartiers(nom, commune);

-- Index sur la zone
CREATE INDEX idx_quartiers_zone_id ON quartiers(zone_id);

-- ==================== CONTRAINTES ====================
-- Un quartier ne peut pas avoir le même nom dans la même commune
CREATE UNIQUE INDEX idx_quartiers_unique_nom_commune ON quartiers(nom, commune);

-- ==================== COMMENTAIRES ====================
COMMENT ON TABLE zones IS 'Zones de livraison avec leurs tarifs (Zone 1, Zone 2, etc.)';
COMMENT ON TABLE quartiers IS 'Quartiers de Dakar associés à des zones';

COMMENT ON COLUMN zones.tarif_standard IS 'Tarif pour livraison NORMAL (24-48h)';
COMMENT ON COLUMN zones.tarif_express IS 'Tarif pour livraison EXPRESS (même jour) - généralement tarif_standard × 1.5';
COMMENT ON COLUMN zones.active IS 'Zone active (true) ou désactivée (false)';
COMMENT ON COLUMN quartiers.actif IS 'Quartier actif (true) ou désactivé (false)';
