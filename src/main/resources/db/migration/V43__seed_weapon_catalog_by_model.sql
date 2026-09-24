-- ADR-0004 (Revisão 2): seed do catálogo com tipo e calibres reais por
-- modelo. Dados de docs/adr/seed-referencia-armas.sql (pesquisa em
-- docs/adr/0004-dados-reais-seed-armas.md). A referência foi escrita para
-- tabelas vazias; aqui ela convive com o seed da V7: o que já existe com o
-- mesmo nome é reaproveitado (ON CONFLICT), e o que a V7 criou fora da
-- referência sai, se não estiver em uso.

-- Calibres da V7 com outro nome na referência: renomeia em vez de duplicar
-- (ids preservados — armas e munições continuam apontando pro mesmo calibre)
UPDATE weapon_calibers SET name = '.38 Special' WHERE name = '.38 SPL';
UPDATE weapon_calibers SET name = '5.56mm NATO' WHERE name = '.223/5.56';
UPDATE weapon_calibers SET name = '12' WHERE name = '12 Gauge';

-- Tipos
INSERT INTO weapon_types (name) VALUES
    ('Pistola'), ('Revólver'), ('Carabina'), ('Espingarda')
ON CONFLICT (name) DO NOTHING;

-- Marcas
INSERT INTO weapon_brands (name) VALUES
    ('Glock'), ('Taurus'), ('Imbel'), ('CBC'), ('Smith & Wesson')
ON CONFLICT (name) DO NOTHING;

-- Calibres
INSERT INTO weapon_calibers (name) VALUES
    ('9mm'), ('.40 S&W'), ('.45 ACP'), ('.22 LR'), ('.357 Magnum'),
    ('.38 Special'), ('.45 Colt'), ('5.56mm NATO'),
    ('12'), ('20'), ('28'), ('.410')
ON CONFLICT (name) DO NOTHING;

-- Modelos: quem já existe (V7) só recebe o tipo
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES
    ('Glock', 'G17', 'Pistola'),
    ('Glock', 'G19', 'Pistola'),
    ('Glock', 'G22', 'Pistola'),
    ('Glock', 'G21', 'Pistola'),
    ('Glock', 'G26', 'Pistola'),

    ('Taurus', 'G2C', 'Pistola'),
    ('Taurus', 'G3', 'Pistola'),
    ('Taurus', 'TX22', 'Pistola'),
    ('Taurus', '605', 'Revólver'),
    ('Taurus', '692', 'Revólver'),
    ('Taurus', '856', 'Revólver'),
    ('Taurus', 'Judge', 'Revólver'),

    ('Imbel', 'IA2', 'Carabina'),
    ('Imbel', 'MD2', 'Carabina'),

    ('CBC', 'Momentum', 'Espingarda'),
    ('CBC', 'Military 3.0', 'Espingarda'),
    ('CBC', 'Montenegro', 'Espingarda'),

    ('Smith & Wesson', 'Model 686', 'Revólver'),
    ('Smith & Wesson', 'Model 10', 'Revólver'),
    ('Smith & Wesson', 'M&P9', 'Pistola'),
    ('Smith & Wesson', 'M&P Shield', 'Pistola')
) AS m(brand_name, name, type_name)
JOIN weapon_brands b ON b.name = m.brand_name
JOIN weapon_types t ON t.name = m.type_name
ON CONFLICT (brand_id, name) DO UPDATE SET weapon_type_id = EXCLUDED.weapon_type_id;

-- Calibres válidos de cada modelo
INSERT INTO weapon_model_calibers (weapon_model_id, weapon_caliber_id)
SELECT wm.id, wc.id
FROM (VALUES
    ('Glock','G17','9mm'), ('Glock','G19','9mm'), ('Glock','G22','.40 S&W'),
    ('Glock','G21','.45 ACP'), ('Glock','G26','9mm'),

    ('Taurus','G2C','9mm'), ('Taurus','G3','9mm'), ('Taurus','TX22','.22 LR'),

    ('Taurus','605','.357 Magnum'), ('Taurus','605','.38 Special'),
    ('Taurus','692','.357 Magnum'), ('Taurus','692','.38 Special'), ('Taurus','692','9mm'),
    ('Taurus','856','.38 Special'),
    ('Taurus','Judge','.45 Colt'),

    ('Imbel','IA2','5.56mm NATO'), ('Imbel','MD2','5.56mm NATO'),

    ('CBC','Momentum','12'), ('CBC','Momentum','20'),
    ('CBC','Military 3.0','12'),
    ('CBC','Montenegro','12'), ('CBC','Montenegro','20'),
    ('CBC','Montenegro','28'), ('CBC','Montenegro','.410'),

    ('Smith & Wesson','Model 686','.357 Magnum'), ('Smith & Wesson','Model 686','.38 Special'),
    ('Smith & Wesson','Model 10','.38 Special'),
    ('Smith & Wesson','M&P9','9mm'), ('Smith & Wesson','M&P Shield','9mm')
) AS combo(brand_name, model_name, caliber_name)
JOIN weapon_brands wb ON wb.name = combo.brand_name
JOIN weapon_models wm ON wm.brand_id = wb.id AND wm.name = combo.model_name
JOIN weapon_calibers wc ON wc.name = combo.caliber_name
ON CONFLICT DO NOTHING;

-- Sobras da V7 fora da referência: saem se nenhuma arma as usa. Se o
-- Rossi RS22 estiver em uso, fica como Carabina .22 LR (dado real) pra
-- não invalidar o cadastro; marca e tipo só saem se ficarem sem uso.
DELETE FROM weapon_models
WHERE name = 'RS22'
  AND brand_id = (SELECT id FROM weapon_brands WHERE name = 'Rossi')
  AND NOT EXISTS (SELECT 1 FROM weapons WHERE weapons.model_id = weapon_models.id);

UPDATE weapon_models
SET weapon_type_id = (SELECT id FROM weapon_types WHERE name = 'Carabina')
WHERE name = 'RS22' AND brand_id = (SELECT id FROM weapon_brands WHERE name = 'Rossi');

INSERT INTO weapon_model_calibers (weapon_model_id, weapon_caliber_id)
SELECT m.id, c.id
FROM weapon_models m
JOIN weapon_brands b ON b.id = m.brand_id
JOIN weapon_calibers c ON c.name = '.22 LR'
WHERE b.name = 'Rossi' AND m.name = 'RS22'
ON CONFLICT DO NOTHING;

DELETE FROM weapon_brands
WHERE name = 'Rossi'
  AND NOT EXISTS (SELECT 1 FROM weapon_models WHERE weapon_models.brand_id = weapon_brands.id)
  AND NOT EXISTS (SELECT 1 FROM weapons WHERE weapons.brand_id = weapon_brands.id);

-- Dados legados (a): tipo e marca de cada arma passam a ser os do modelo
UPDATE weapons
SET type_id = weapon_models.weapon_type_id,
    brand_id = weapon_models.brand_id
FROM weapon_models
WHERE weapons.model_id = weapon_models.id
  AND (weapons.type_id <> weapon_models.weapon_type_id OR weapons.brand_id <> weapon_models.brand_id);

-- 'Outro' (V7) não tem modelo; depois do ajuste acima, nenhuma arma usa
-- mais — sai do catálogo
DELETE FROM weapon_types
WHERE name = 'Outro'
  AND NOT EXISTS (SELECT 1 FROM weapon_models WHERE weapon_models.weapon_type_id = weapon_types.id)
  AND NOT EXISTS (SELECT 1 FROM weapons WHERE weapons.type_id = weapon_types.id);

-- Dados legados (b): toda combinação modelo+calibre já usada por alguma
-- arma continua válida, mesmo fora da referência — não invalida cadastro feito
INSERT INTO weapon_model_calibers (weapon_model_id, weapon_caliber_id)
SELECT DISTINCT model_id, caliber_id FROM weapons
ON CONFLICT DO NOTHING;

ALTER TABLE weapon_models ALTER COLUMN weapon_type_id SET NOT NULL;
