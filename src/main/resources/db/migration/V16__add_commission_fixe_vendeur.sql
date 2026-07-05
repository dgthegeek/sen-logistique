-- V16__add_commission_fixe_vendeur.sql
-- Commission fixe par partenaire : sert de prix de livraison fixe pour chaque
-- commande du vendeur (réglée par l'admin à la validation). NULL = pas encore fixée.

ALTER TABLE vendeurs ADD COLUMN commission_fixe DECIMAL(12,2);

COMMENT ON COLUMN vendeurs.commission_fixe IS 'Prix de livraison fixe (commission) du partenaire, réglé par l''admin';
