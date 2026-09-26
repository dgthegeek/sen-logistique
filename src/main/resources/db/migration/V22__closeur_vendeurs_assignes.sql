-- V22__closeur_vendeurs_assignes.sql
-- Permet de restreindre un closeur à un sous-ensemble de vendeurs : il ne
-- voit et ne peut fermer que les commandes de ces vendeurs. Aucune ligne
-- pour un closeur donné = pas de restriction (il voit tous les vendeurs,
-- comportement identique à avant cette migration).
CREATE TABLE closeur_vendeurs (
    closeur_id BIGINT NOT NULL REFERENCES closeurs(id) ON DELETE CASCADE,
    vendeur_id BIGINT NOT NULL REFERENCES vendeurs(id) ON DELETE CASCADE,
    PRIMARY KEY (closeur_id, vendeur_id)
);

COMMENT ON TABLE closeur_vendeurs IS
    'Vendeurs auxquels un closeur est restreint. Aucune ligne pour un closeur = pas de restriction (tous les vendeurs).';
