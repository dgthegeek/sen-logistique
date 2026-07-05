-- V18__livraison_adresse_libre.sql
-- Adresses de livraison en saisie libre : commune, quartier et zone ne sont plus
-- obligatoires (seule l'adresse complète l'est). Le prix = commission fixe du vendeur.

ALTER TABLE livraisons ALTER COLUMN commune DROP NOT NULL;
ALTER TABLE livraisons ALTER COLUMN quartier DROP NOT NULL;
ALTER TABLE livraisons ALTER COLUMN zone_id DROP NOT NULL;
