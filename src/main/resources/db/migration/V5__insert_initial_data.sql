-- V5__insert_initial_data.sql
-- Insertion des zones et quartiers de Dakar

-- ==================== INSERTION DES ZONES ====================

-- Zone 1 : Centre-ville (tarif le plus bas)
INSERT INTO zones (nom, description, tarif_standard, tarif_express, active) VALUES
('Zone 1', 'Plateau, Ponty, Médina, Centre-ville', 1000.00, 1500.00, true);

-- Zone 2 : Proches banlieues
INSERT INTO zones (nom, description, tarif_standard, tarif_express, active) VALUES
('Zone 2', 'Sacré-Cœur, Mermoz, Fann, Amitié, Point E', 1500.00, 2250.00, true);

-- Zone 3 : Banlieues
INSERT INTO zones (nom, description, tarif_standard, tarif_express, active) VALUES
('Zone 3', 'Parcelles Assainies, Grand Yoff, Pikine, Guédiawaye', 2000.00, 3000.00, true);

-- Zone 4 : Lointaines banlieues
INSERT INTO zones (nom, description, tarif_standard, tarif_express, active) VALUES
('Zone 4', 'Rufisque, Thiaroye, Keur Massar, Sangalkam', 2500.00, 3750.00, true);

-- ==================== INSERTION DES QUARTIERS ====================

-- Quartiers de la Zone 1
INSERT INTO quartiers (nom, commune, zone_id, actif) VALUES
-- Plateau
('Plateau', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true),
('Ponty', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true),
('Rebeuss', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true),

-- Médina
('Médina', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true),
('Gueule Tapée', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true),
('Fass', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true),

-- Centre-ville
('HLM', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true),
('Dieuppeul', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 1'), true);

-- Quartiers de la Zone 2
INSERT INTO quartiers (nom, commune, zone_id, actif) VALUES
-- Sacré-Cœur / Mermoz
('Sacré-Cœur', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Mermoz', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('SICAP', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Fann', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Amitié', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Point E', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),

-- Almadies
('Almadies', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Ngor', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Yoff', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Ouakam', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),

-- Liberté
('Liberté 1', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Liberté 2', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Liberté 3', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Liberté 4', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Liberté 5', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true),
('Liberté 6', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 2'), true);

-- Quartiers de la Zone 3
INSERT INTO quartiers (nom, commune, zone_id, actif) VALUES
-- Parcelles Assainies
('Unité 1', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 2', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 3', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 4', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 5', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 6', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 7', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 8', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 9', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 10', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Unité 11', 'Parcelles Assainies', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),

-- Grand Yoff
('Grand Yoff', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Patte d''Oie', 'Dakar', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),

-- Pikine
('Pikine Ancien', 'Pikine', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Pikine Rue 10', 'Pikine', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Guinaw Rail', 'Pikine', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Thiaroye', 'Pikine', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),

-- Guédiawaye
('Médina Gounass', 'Guédiawaye', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Sam Notaire', 'Guédiawaye', (SELECT id FROM zones WHERE nom = 'Zone 3'), true),
('Golf Sud', 'Guédiawaye', (SELECT id FROM zones WHERE nom = 'Zone 3'), true);

-- Quartiers de la Zone 4
INSERT INTO quartiers (nom, commune, zone_id, actif) VALUES
-- Rufisque
('Rufisque Est', 'Rufisque', (SELECT id FROM zones WHERE nom = 'Zone 4'), true),
('Rufisque Ouest', 'Rufisque', (SELECT id FROM zones WHERE nom = 'Zone 4'), true),
('Bargny', 'Rufisque', (SELECT id FROM zones WHERE nom = 'Zone 4'), true),

-- Keur Massar
('Keur Massar', 'Keur Massar', (SELECT id FROM zones WHERE nom = 'Zone 4'), true),
('Malika', 'Keur Massar', (SELECT id FROM zones WHERE nom = 'Zone 4'), true),
('Yeumbeul', 'Keur Massar', (SELECT id FROM zones WHERE nom = 'Zone 4'), true),

-- Autres
('Sangalkam', 'Rufisque', (SELECT id FROM zones WHERE nom = 'Zone 4'), true),
('Diamniadio', 'Rufisque', (SELECT id FROM zones WHERE nom = 'Zone 4'), true);

-- ==================== VÉRIFICATION ====================
-- Afficher le nombre de zones et quartiers insérés
DO $$
DECLARE
    zone_count INTEGER;
    quartier_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO zone_count FROM zones;
    SELECT COUNT(*) INTO quartier_count FROM quartiers;
    
    RAISE NOTICE 'Données initiales insérées avec succès:';
    RAISE NOTICE '  - % zones créées', zone_count;
    RAISE NOTICE '  - % quartiers créés', quartier_count;
END $$;
