-- V13__add_classement_gamification.sql
-- Dioks League : classement gamifié opt-in entre vendeurs.
-- Les statistiques (nombre de livraisons, CA) sont calculées à la volée sur les
-- livraisons réelles : quitter puis revenir ne remet jamais les données à zéro.

ALTER TABLE vendeurs
    ADD COLUMN participe_classement BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE vendeurs
    ADD COLUMN date_adhesion_classement TIMESTAMP;

COMMENT ON COLUMN vendeurs.participe_classement IS 'Le vendeur participe à la Dioks League (visible par les autres participants)';
COMMENT ON COLUMN vendeurs.date_adhesion_classement IS 'Date de première/dernière adhésion à la Dioks League';
