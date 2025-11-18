-- V1__create_users_tables.sql
-- Création des tables pour les utilisateurs (User, Vendeur, Admin)
-- Stratégie d'héritage : JOINED (3 tables séparées)

-- ==================== TABLE USERS (parent) ====================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(150),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('VENDEUR', 'ADMIN')),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_inscription TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index sur le téléphone pour l'authentification (très utilisé)
CREATE INDEX idx_users_telephone ON users(telephone);

-- Index sur l'email
CREATE INDEX idx_users_email ON users(email);

-- Index sur le rôle pour les recherches par type
CREATE INDEX idx_users_role ON users(role);

-- ==================== TABLE VENDEURS (hérite de users) ====================
CREATE TABLE vendeurs (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    nom_boutique VARCHAR(200),
    categorie_activite VARCHAR(100),
    instagram VARCHAR(100),
    facebook VARCHAR(200),
    commune VARCHAR(100),
    quartier VARCHAR(100),
    adresse_complete TEXT,
    solde_en_attente NUMERIC(12, 2) NOT NULL DEFAULT 0.00
);

-- Index sur le quartier pour les ramassages groupés
CREATE INDEX idx_vendeurs_quartier ON vendeurs(quartier);

-- Index sur la commune
CREATE INDEX idx_vendeurs_commune ON vendeurs(commune);

-- Index sur le solde pour trouver les vendeurs à payer
CREATE INDEX idx_vendeurs_solde ON vendeurs(solde_en_attente);

-- ==================== TABLE ADMINS (hérite de users) ====================
CREATE TABLE admins (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
    -- Pas de champs supplémentaires pour le MVP
);

-- ==================== COMMENTAIRES ====================
COMMENT ON TABLE users IS 'Table parent pour tous les utilisateurs (Vendeur et Admin)';
COMMENT ON TABLE vendeurs IS 'Vendeurs Instagram/Facebook qui créent des livraisons';
COMMENT ON TABLE admins IS 'Administrateurs qui gèrent les ramassages et livraisons';

COMMENT ON COLUMN vendeurs.solde_en_attente IS 'Montant que le vendeur a gagné mais pas encore reçu';
COMMENT ON COLUMN users.actif IS 'Compte actif (true) ou désactivé (false)';
