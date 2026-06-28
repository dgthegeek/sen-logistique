-- V7__add_closing_dispatch.sql
-- Modules Closing & Dispatch :
--   - Nouveaux rôles CLOSEUR et LIVREUR
--   - Tables d'héritage closeurs / livreurs (stratégie JOINED)
--   - Nouveau cycle de vie des commandes + assignation à un livreur
-- Migration ADDITIVE : les anciens statuts (module ramassage) restent valides (dormant).

-- ==================== RÔLES : étendre la contrainte ====================
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('VENDEUR', 'ADMIN', 'CLOSEUR', 'LIVREUR'));

-- ==================== TABLE CLOSEURS (hérite de users) ====================
CREATE TABLE closeurs (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

-- ==================== TABLE LIVREURS (hérite de users) ====================
CREATE TABLE livreurs (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    zone_preferee VARCHAR(100)
);

-- ==================== LIVRAISONS : colonnes Closing & Dispatch ====================
ALTER TABLE livraisons ADD COLUMN livreur_id BIGINT REFERENCES livreurs(id) ON DELETE SET NULL;
ALTER TABLE livraisons ADD COLUMN date_confirmation TIMESTAMP;
ALTER TABLE livraisons ADD COLUMN date_assignation TIMESTAMP;
ALTER TABLE livraisons ADD COLUMN motif_echec VARCHAR(30)
    CHECK (motif_echec IN (
        'TELEPHONE_INJOIGNABLE',
        'CLIENT_ABSENT',
        'ADRESSE_INCORRECTE',
        'REFUS_CLIENT',
        'REPORT_CLIENT'
    ));

-- Index pour récupérer "Mes livraisons" d'un livreur
CREATE INDEX idx_livraisons_livreur_id ON livraisons(livreur_id);
CREATE INDEX idx_livraisons_livreur_statut ON livraisons(livreur_id, statut);

-- ==================== STATUT : étendre la contrainte ====================
-- On conserve les anciens statuts (ramassage dormant) et on ajoute le nouveau cycle.
ALTER TABLE livraisons DROP CONSTRAINT IF EXISTS livraisons_statut_check;
ALTER TABLE livraisons
    ADD CONSTRAINT livraisons_statut_check
    CHECK (statut IN (
        -- Nouveau cycle (Closing + Dispatch)
        'NOUVELLE',
        'A_APPELER',
        'CONFIRMEE',
        'PRETE_A_LIVRER',
        'ASSIGNEE',
        'EN_LIVRAISON',
        'LIVREE',
        'ECHEC',
        'ANNULEE',
        -- Ancien cycle (ramassage - dormant, conservé pour l'historique)
        'EN_ATTENTE_RAMASSAGE',
        'RAMASSE',
        'EN_ROUTE',
        'ECHEC_ABSENT',
        'ECHEC_REFUSE'
    ));

-- ==================== COMMENTAIRES ====================
COMMENT ON TABLE closeurs IS 'Closeurs / assistantes : confirment les commandes (module Closing)';
COMMENT ON TABLE livreurs IS 'Livreurs : exécutent les livraisons assignées (module Dispatch)';
COMMENT ON COLUMN livraisons.livreur_id IS 'Livreur assigné à la livraison (module Dispatch)';
COMMENT ON COLUMN livraisons.motif_echec IS 'Motif obligatoire lorsque le statut = ECHEC';
