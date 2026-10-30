-- V23__tracking_number_counters.sql
-- Génération atomique des numéros de tracking (DKR-YYYYMMDD-NNNNN).
-- L'ancien générateur comptait les livraisons du jour puis ajoutait 1 :
-- deux créations de commande concurrentes pouvaient lire le même compte
-- et calculer le même numéro, provoquant une violation de la contrainte
-- unique "livraisons_numero_tracking_key" (vu en prod). Un compteur
-- dédié, incrémenté par un UPSERT atomique (INSERT ... ON CONFLICT DO
-- UPDATE ... RETURNING), élimine la race : Postgres sérialise les accès
-- concurrents à la même ligne.
CREATE TABLE tracking_number_counters (
    jour DATE PRIMARY KEY,
    dernier_numero INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE tracking_number_counters IS
    'Compteur quotidien pour générer les numéros de tracking (DKR-YYYYMMDD-NNNNN) sans race condition.';
