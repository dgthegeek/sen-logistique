-- Ajout des colonnes de statut et validation
ALTER TABLE vendeurs
    ADD COLUMN statut VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION'
        CHECK (statut IN ('EN_ATTENTE_VALIDATION', 'ACTIF', 'SUSPENDU', 'BLOQUE'));

ALTER TABLE vendeurs
    ADD COLUMN valide_par BIGINT REFERENCES admins(id);

ALTER TABLE vendeurs
    ADD COLUMN valide_le TIMESTAMP;

ALTER TABLE vendeurs
    ADD COLUMN raison_suspension TEXT;

-- Index pour performance sur les requêtes fréquentes
CREATE INDEX idx_vendeurs_statut ON vendeurs(statut);

-- Les vendeurs existants passent directement ACTIF
UPDATE vendeurs SET statut = 'ACTIF' WHERE statut = 'EN_ATTENTE_VALIDATION';