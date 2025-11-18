-- V3__create_livraisons_table.sql
-- Création de la table livraisons (la plus importante du système)

-- ==================== TABLE LIVRAISONS ====================
CREATE TABLE livraisons (
    id BIGSERIAL PRIMARY KEY,
    
    -- Tracking
    numero_tracking VARCHAR(50) UNIQUE NOT NULL,
    qr_code_url VARCHAR(500),
    
    -- Relations
    vendeur_id BIGINT NOT NULL REFERENCES vendeurs(id) ON DELETE RESTRICT,
    
    -- Client final
    nom_client VARCHAR(200) NOT NULL,
    telephone_client VARCHAR(20) NOT NULL,
    
    -- Adresse (Embedded) - colonnes de l'objet Adresse
    commune VARCHAR(100) NOT NULL,
    quartier VARCHAR(100) NOT NULL,
    adresse_complete TEXT NOT NULL,
    point_repere VARCHAR(255),
    zone_id BIGINT NOT NULL REFERENCES zones(id) ON DELETE RESTRICT,
    
    -- Détails du colis
    description_produit TEXT NOT NULL,
    fragile BOOLEAN NOT NULL DEFAULT FALSE,
    poids NUMERIC(10, 2),
    
    -- Finances
    montant_cod NUMERIC(12, 2) NOT NULL,
    frais_livraison NUMERIC(10, 2) NOT NULL,
    cash_collecte NUMERIC(12, 2),
    
    -- Statut et dates
    statut VARCHAR(30) NOT NULL CHECK (statut IN (
        'EN_ATTENTE_RAMASSAGE',
        'RAMASSE',
        'EN_ROUTE',
        'LIVREE',
        'ECHEC_ABSENT',
        'ECHEC_REFUSE',
        'ANNULEE'
    )),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_ramassage TIMESTAMP,
    date_en_route TIMESTAMP,
    date_livraison TIMESTAMP,
    
    -- Options
    urgence VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (urgence IN ('NORMAL', 'EXPRESS')),
    creneau_souhaite VARCHAR(20),
    notes_pour_livreur VARCHAR(500),
    
    -- Après livraison
    commentaire_livraison TEXT,
    latitude_livraison NUMERIC(10, 8),
    longitude_livraison NUMERIC(11, 8)
);

-- ==================== INDEX IMPORTANTS ====================

-- Index sur le numéro de tracking (TRÈS utilisé pour le tracking public)
CREATE INDEX idx_livraisons_numero_tracking ON livraisons(numero_tracking);

-- Index sur le vendeur (pour récupérer les livraisons d'un vendeur)
CREATE INDEX idx_livraisons_vendeur_id ON livraisons(vendeur_id);

-- Index sur le statut (pour filtrer par statut)
CREATE INDEX idx_livraisons_statut ON livraisons(statut);

-- Index composite vendeur + statut (très utilisé ensemble)
CREATE INDEX idx_livraisons_vendeur_statut ON livraisons(vendeur_id, statut);

-- Index sur la date de création (pour les statistiques et rapports)
CREATE INDEX idx_livraisons_date_creation ON livraisons(date_creation);

-- Index sur la date de livraison (pour les rapports journaliers)
CREATE INDEX idx_livraisons_date_livraison ON livraisons(date_livraison);

-- Index sur le quartier de destination (pour les tournées de livraison)
CREATE INDEX idx_livraisons_quartier_dest ON livraisons(quartier);

-- Index sur la zone (pour grouper par zone)
CREATE INDEX idx_livraisons_zone_id ON livraisons(zone_id);

-- Index composite statut + date_creation (pour ramassages ordonnés)
CREATE INDEX idx_livraisons_statut_date ON livraisons(statut, date_creation);

-- Index composite statut + date_ramassage (pour livraisons à effectuer)
CREATE INDEX idx_livraisons_statut_ramassage ON livraisons(statut, date_ramassage);

-- ==================== COMMENTAIRES ====================
COMMENT ON TABLE livraisons IS 'Cœur du système - Toutes les livraisons créées par les vendeurs';

COMMENT ON COLUMN livraisons.numero_tracking IS 'Numéro unique de tracking (ex: DKR-00567)';
COMMENT ON COLUMN livraisons.qr_code_url IS 'URL du QR code pour la confirmation de livraison';
COMMENT ON COLUMN livraisons.montant_cod IS 'Montant Cash on Delivery que le client doit payer';
COMMENT ON COLUMN livraisons.frais_livraison IS 'Frais de livraison calculés selon la zone et l''urgence';
COMMENT ON COLUMN livraisons.cash_collecte IS 'Cash réellement collecté lors de la livraison (doit = montant_cod)';
COMMENT ON COLUMN livraisons.statut IS 'Statut actuel de la livraison';
COMMENT ON COLUMN livraisons.urgence IS 'Type de livraison : NORMAL (24-48h) ou EXPRESS (même jour)';
COMMENT ON COLUMN livraisons.fragile IS 'Indique si le colis est fragile';
COMMENT ON COLUMN livraisons.poids IS 'Poids du colis en kg (optionnel)';
