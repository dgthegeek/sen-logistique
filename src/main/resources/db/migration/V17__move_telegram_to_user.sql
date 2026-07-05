-- V17__move_telegram_to_user.sql
-- Liaison Telegram disponible pour TOUS les rôles (vendeur, closeur, dispatcheur,
-- livreur, admin) : on remonte les colonnes de la table `vendeurs` vers `users`.

ALTER TABLE users ADD COLUMN telegram_chat_id VARCHAR(50);
ALTER TABLE users ADD COLUMN telegram_link_code VARCHAR(40);

-- Reprise des liaisons Telegram déjà existantes (vendeurs)
UPDATE users u
SET telegram_chat_id = v.telegram_chat_id,
    telegram_link_code = v.telegram_link_code
FROM vendeurs v
WHERE v.id = u.id
  AND (v.telegram_chat_id IS NOT NULL OR v.telegram_link_code IS NOT NULL);

ALTER TABLE vendeurs DROP COLUMN telegram_chat_id;
ALTER TABLE vendeurs DROP COLUMN telegram_link_code;

COMMENT ON COLUMN users.telegram_chat_id IS 'Chat Telegram lié (null tant que non lié)';
COMMENT ON COLUMN users.telegram_link_code IS 'Code temporaire de liaison (deep link t.me/bot?start=code)';
