-- V11__create_lignes_commande.sql
-- Lignes de commande : plusieurs produits par livraison (multi-produits).

CREATE TABLE lignes_commande (
    id BIGSERIAL PRIMARY KEY,
    livraison_id BIGINT NOT NULL REFERENCES livraisons(id) ON DELETE CASCADE,
    produit_id BIGINT NOT NULL REFERENCES produits(id) ON DELETE RESTRICT,
    quantite INTEGER NOT NULL,
    prix_unitaire NUMERIC(12, 2)
);

CREATE INDEX idx_lignes_livraison ON lignes_commande(livraison_id);
CREATE INDEX idx_lignes_produit ON lignes_commande(produit_id);

COMMENT ON TABLE lignes_commande IS 'Produits commandes au sein d''une livraison (multi-produits)';
COMMENT ON COLUMN lignes_commande.prix_unitaire IS 'Prix unitaire applique (snapshot catalogue)';
