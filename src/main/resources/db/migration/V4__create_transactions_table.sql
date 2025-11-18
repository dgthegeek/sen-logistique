-- V4__create_transactions_table.sql
-- Création de la table transactions (historique des paiements)

-- ==================== TABLE TRANSACTIONS ====================
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    
    -- Relation
    vendeur_id BIGINT NOT NULL REFERENCES vendeurs(id) ON DELETE RESTRICT,
    
    -- Détails de la transaction
    montant NUMERIC(12, 2) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN (
        'PAIEMENT_VENDEUR',
        'COLLECTE_COD',
        'COMMISSION'
    )),
    reference VARCHAR(100) UNIQUE NOT NULL,
    statut VARCHAR(20) NOT NULL CHECK (statut IN (
        'EFFECTUE',
        'EN_ATTENTE',
        'ANNULE'
    )),
    
    -- Informations complémentaires
    commentaire TEXT,
    date_transaction TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    admin_nom VARCHAR(100)
);

-- ==================== INDEX ====================

-- Index sur la référence (unique et recherchée souvent)
CREATE INDEX idx_transactions_reference ON transactions(reference);

-- Index sur le vendeur (pour l'historique)
CREATE INDEX idx_transactions_vendeur_id ON transactions(vendeur_id);

-- Index sur le type (pour filtrer par type)
CREATE INDEX idx_transactions_type ON transactions(type);

-- Index sur le statut (pour trouver les paiements en attente)
CREATE INDEX idx_transactions_statut ON transactions(statut);

-- Index sur la date (pour les rapports)
CREATE INDEX idx_transactions_date ON transactions(date_transaction);

-- Index composite vendeur + type (souvent utilisés ensemble)
CREATE INDEX idx_transactions_vendeur_type ON transactions(vendeur_id, type);

-- Index composite vendeur + statut
CREATE INDEX idx_transactions_vendeur_statut ON transactions(vendeur_id, statut);

-- Index composite type + statut (pour calculer les commissions effectuées)
CREATE INDEX idx_transactions_type_statut ON transactions(type, statut);

-- ==================== COMMENTAIRES ====================
COMMENT ON TABLE transactions IS 'Historique de toutes les transactions financières (paiements vendeurs, commissions, etc.)';

COMMENT ON COLUMN transactions.type IS 'Type de transaction : PAIEMENT_VENDEUR (cash remis), COLLECTE_COD (cash collecté), COMMISSION (commission plateforme)';
COMMENT ON COLUMN transactions.reference IS 'Référence unique de la transaction (ex: PAY-20251116-001)';
COMMENT ON COLUMN transactions.statut IS 'Statut du paiement : EFFECTUE, EN_ATTENTE, ou ANNULE';
COMMENT ON COLUMN transactions.montant IS 'Montant de la transaction en FCFA';
COMMENT ON COLUMN transactions.admin_nom IS 'Nom de l''admin qui a effectué le paiement';
