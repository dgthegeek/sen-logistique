-- V8__create_stock_tables.sql
-- Module Stock (DIOKS) : produits des partenaires + journal des mouvements de stock.

-- ==================== TABLE PRODUITS ====================
CREATE TABLE produits (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    nom VARCHAR(200) NOT NULL,
    description TEXT,
    vendeur_id BIGINT NOT NULL REFERENCES vendeurs(id) ON DELETE RESTRICT,
    prix_unitaire NUMERIC(12, 2),
    quantite_stock INTEGER NOT NULL DEFAULT 0,
    seuil_alerte INTEGER NOT NULL DEFAULT 5,
    qr_code_url VARCHAR(500),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_produits_code ON produits(code);
CREATE INDEX idx_produits_vendeur ON produits(vendeur_id);
CREATE INDEX idx_produits_actif ON produits(actif);

-- ==================== TABLE MOUVEMENTS_STOCK ====================
CREATE TABLE mouvements_stock (
    id BIGSERIAL PRIMARY KEY,
    produit_id BIGINT NOT NULL REFERENCES produits(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ENTREE', 'SORTIE', 'AJUSTEMENT', 'CREATION')),
    variation INTEGER NOT NULL,
    stock_avant INTEGER NOT NULL,
    stock_apres INTEGER NOT NULL,
    livraison_id BIGINT REFERENCES livraisons(id) ON DELETE SET NULL,
    commentaire TEXT,
    auteur VARCHAR(100),
    date_mouvement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mouvements_produit ON mouvements_stock(produit_id);
CREATE INDEX idx_mouvements_date ON mouvements_stock(date_mouvement);
CREATE INDEX idx_mouvements_type ON mouvements_stock(type);

COMMENT ON TABLE produits IS 'Marchandise des partenaires stockée au warehouse';
COMMENT ON TABLE mouvements_stock IS 'Journal des entrées/sorties/ajustements de stock';
COMMENT ON COLUMN mouvements_stock.variation IS 'Variation signée: positive (entrée) ou négative (sortie)';
