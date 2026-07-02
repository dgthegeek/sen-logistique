-- V12__add_dispatcheur.sql
-- Nouveau rôle DISPATCHEUR : prépare les commandes "Prête à livrer" et les assigne
-- à un livreur (module Dispatch). Table d'héritage JOINED comme closeurs/livreurs.

-- ==================== RÔLES : étendre la contrainte ====================
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('VENDEUR', 'ADMIN', 'CLOSEUR', 'LIVREUR', 'DISPATCHEUR'));

-- ==================== TABLE DISPATCHEURS (hérite de users) ====================
CREATE TABLE dispatcheurs (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE dispatcheurs IS 'Dispatcheurs : préparent et assignent les commandes prêtes aux livreurs (module Dispatch)';
