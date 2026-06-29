-- V10__add_date_echec.sql
-- Date d'échec d'une livraison, pour le bilan quotidien (échecs du jour).

ALTER TABLE livraisons ADD COLUMN date_echec TIMESTAMP;

COMMENT ON COLUMN livraisons.date_echec IS 'Date de passage au statut ECHEC';
