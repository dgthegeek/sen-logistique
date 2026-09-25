-- V21__partner_api_key_and_order_origin.sql
-- Intégrations externes (Shopify, Zapier, scripts maison) : un vendeur peut
-- générer une clé API pour laisser un système tiers créer des commandes en
-- son nom, sans authentification par mot de passe.

-- ==================== CLÉ API VENDEUR ====================
ALTER TABLE vendeurs ADD COLUMN api_key VARCHAR(80);

-- Une clé API ne doit jamais être partagée entre deux vendeurs.
ALTER TABLE vendeurs ADD CONSTRAINT vendeurs_api_key_key UNIQUE (api_key);

COMMENT ON COLUMN vendeurs.api_key IS
    'Clé API permettant à un système externe (Shopify, script...) de créer des commandes au nom de ce vendeur, sans session. Null si aucune intégration configurée.';

-- ==================== ORIGINE DE LA COMMANDE (traçabilité + idempotence) ====================
-- Permet de savoir d'où vient une commande (UI vendeur/admin = null, API
-- partenaire générique, ou Shopify) et d'éviter les doublons si un webhook
-- externe est redélivré (Shopify notamment redélivre en cas de non-réponse).
ALTER TABLE livraisons ADD COLUMN origine VARCHAR(30);
ALTER TABLE livraisons ADD COLUMN origine_ref VARCHAR(150);

COMMENT ON COLUMN livraisons.origine IS
    'Origine de la commande : null (créée depuis l''interface), API (intégration générique), SHOPIFY (webhook Shopify).';
COMMENT ON COLUMN livraisons.origine_ref IS
    'Identifiant de la commande côté système externe (ex. ID commande Shopify, ou clé d''idempotence fournie par le partenaire). Sert à ne jamais créer deux fois la même commande.';

-- Un même (vendeur, origine, origine_ref) ne doit jamais produire deux
-- commandes. Index partiel : ne s'applique qu'aux commandes réellement
-- issues d'une intégration externe (origine_ref non nul).
CREATE UNIQUE INDEX idx_livraisons_origine_unique
    ON livraisons (vendeur_id, origine, origine_ref)
    WHERE origine_ref IS NOT NULL;
