-- V9__link_livraison_produit.sql
-- Lien optionnel entre une livraison et un produit du stock.
-- Permet le décrément automatique du stock à la livraison.

ALTER TABLE livraisons ADD COLUMN produit_id BIGINT REFERENCES produits(id) ON DELETE SET NULL;
ALTER TABLE livraisons ADD COLUMN quantite INTEGER;

CREATE INDEX idx_livraisons_produit_id ON livraisons(produit_id);

COMMENT ON COLUMN livraisons.produit_id IS 'Produit lié (optionnel) - décrément auto du stock à la livraison';
COMMENT ON COLUMN livraisons.quantite IS 'Quantité commandée du produit lié';
