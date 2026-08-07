-- V19__add_versement_livreur.sql
-- Suivi de l'argent (cash COD) collecté par chaque livreur et qu'il doit reverser
-- au coordinateur logistique / à l'admin.
--
-- Principe :
--   * Chaque livraison LIVREE porte un cash_collecte encaissé par le livreur.
--   * Tant que ce cash n'a pas été reversé, verse_livreur = false → il compte
--     dans le "solde à régler" du livreur.
--   * Quand le coordinateur/admin marque le livreur comme "versé", on crée un
--     enregistrement dans versements_livreur (trace horodatée), on rattache
--     toutes les livraisons non réglées à ce versement et on passe verse_livreur
--     = true → le solde du livreur retombe à zéro.

-- ==================== TABLE DES VERSEMENTS (historique) ====================
CREATE TABLE versements_livreur (
    id                 BIGSERIAL PRIMARY KEY,
    livreur_id         BIGINT NOT NULL REFERENCES livreurs(id) ON DELETE CASCADE,
    montant            NUMERIC(14, 2) NOT NULL,
    nombre_livraisons  INTEGER NOT NULL DEFAULT 0,
    date_versement     TIMESTAMP NOT NULL DEFAULT NOW(),
    effectue_par       VARCHAR(150),
    effectue_par_role  VARCHAR(30),
    commentaire        TEXT
);

CREATE INDEX idx_versements_livreur_livreur ON versements_livreur (livreur_id);
CREATE INDEX idx_versements_livreur_date ON versements_livreur (date_versement);

COMMENT ON TABLE versements_livreur IS
    'Historique des versements de cash (COD) d''un livreur vers le coordinateur/admin. Chaque versement remet le solde du livreur à zéro.';

-- ==================== LIVRAISONS : statut de règlement ====================
ALTER TABLE livraisons
    ADD COLUMN verse_livreur BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN date_versement_livreur TIMESTAMP,
    ADD COLUMN versement_id BIGINT REFERENCES versements_livreur(id) ON DELETE SET NULL;

CREATE INDEX idx_livraisons_versement ON livraisons (versement_id);
-- Index partiel : accélère le calcul du solde à régler (livraisons non versées).
CREATE INDEX idx_livraisons_non_verse ON livraisons (livreur_id) WHERE verse_livreur = FALSE;

COMMENT ON COLUMN livraisons.verse_livreur IS
    'true = le cash collecté sur cette livraison a été reversé au coordinateur/admin.';
