-- V14__add_tracabilite_qualite.sql
-- Traçabilité qualité : qui a pris en charge / dispatché une commande, et à quels moments.

ALTER TABLE livraisons ADD COLUMN closeur_id BIGINT REFERENCES closeurs(id) ON DELETE SET NULL;
ALTER TABLE livraisons ADD COLUMN dispatcheur_id BIGINT REFERENCES dispatcheurs(id) ON DELETE SET NULL;
ALTER TABLE livraisons ADD COLUMN date_prise_en_charge TIMESTAMP;
ALTER TABLE livraisons ADD COLUMN date_prete_a_livrer TIMESTAMP;

CREATE INDEX idx_livraisons_closeur_id ON livraisons(closeur_id);
CREATE INDEX idx_livraisons_dispatcheur_id ON livraisons(dispatcheur_id);

COMMENT ON COLUMN livraisons.closeur_id IS 'Closeur ayant pris la commande en charge';
COMMENT ON COLUMN livraisons.dispatcheur_id IS 'Dispatcheur ayant assigné la commande';
COMMENT ON COLUMN livraisons.date_prise_en_charge IS 'Première prise en charge par le closeur';
COMMENT ON COLUMN livraisons.date_prete_a_livrer IS 'Passage au statut PRETE_A_LIVRER (fin closing)';
