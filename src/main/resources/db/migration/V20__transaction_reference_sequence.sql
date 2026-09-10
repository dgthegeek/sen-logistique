-- V20__transaction_reference_sequence.sql
-- Corrige une collision de référence lors de paiements vendeurs rapprochés :
-- la référence était générée par "COMPTER les transactions du jour + 1", ce qui
-- n'est pas sûr en cas d'appels concurrents (plusieurs requêtes lisent le même
-- compteur avant qu'aucune n'ait validé -> même référence -> erreur de
-- contrainte unique sur transactions.reference, paiement rejeté).
--
-- Une séquence Postgres est atomique par nature : deux appels concurrents à
-- nextval() ne peuvent jamais renvoyer la même valeur, sans verrou explicite.
CREATE SEQUENCE IF NOT EXISTS transaction_reference_seq START WITH 1 INCREMENT BY 1;
