-- V15__add_telegram_vendeur.sql
-- Notifications Telegram : lien du compte vendeur à une conversation Telegram.

ALTER TABLE vendeurs ADD COLUMN telegram_chat_id VARCHAR(50);
ALTER TABLE vendeurs ADD COLUMN telegram_link_code VARCHAR(40);

CREATE INDEX idx_vendeurs_telegram_link_code ON vendeurs(telegram_link_code);

COMMENT ON COLUMN vendeurs.telegram_chat_id IS 'Chat ID Telegram du vendeur (après liaison du bot)';
COMMENT ON COLUMN vendeurs.telegram_link_code IS 'Code de liaison Telegram (deep link start=code)';
