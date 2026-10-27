-- Seed com dados reais de fabricante — referência pra migration do
-- ADR-0004 (Revisão 2). Ajuste o número de versão do Flyway (V<n>__)
-- conforme a sequência real do projeto.

-- Tipos
INSERT INTO weapon_types (name) VALUES
                                    ('Pistola'), ('Revólver'), ('Carabina'), ('Espingarda');

-- Marcas
INSERT INTO weapon_brands (name) VALUES
                                     ('Glock'), ('Taurus'), ('Imbel'), ('CBC'), ('Smith & Wesson');

-- Calibres
INSERT INTO weapon_calibers (name) VALUES
                                       ('9mm'), ('.40 S&W'), ('.45 ACP'), ('.22 LR'), ('.357 Magnum'),
                                       ('.38 Special'), ('.45 Colt'), ('5.56mm NATO'),
                                       ('12'), ('20'), ('28'), ('.410');

-- Modelos Glock (todos Pistola)
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES ('G17'), ('G19'), ('G22'), ('G21'), ('G26')) AS m(name)
         CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'Glock') b
         CROSS JOIN (SELECT id FROM weapon_types WHERE name = 'Pistola') t;

-- Modelos Taurus — Pistola
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES ('G2C'), ('G3'), ('TX22')) AS m(name)
         CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'Taurus') b
         CROSS JOIN (SELECT id FROM weapon_types WHERE name = 'Pistola') t;

-- Modelos Taurus — Revólver
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES ('605'), ('692'), ('856'), ('Judge')) AS m(name)
         CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'Taurus') b
         CROSS JOIN (SELECT id FROM weapon_types WHERE name = 'Revólver') t;

-- Modelos Imbel — Carabina
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES ('IA2'), ('MD2')) AS m(name)
         CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'Imbel') b
         CROSS JOIN (SELECT id FROM weapon_types WHERE name = 'Carabina') t;

-- Modelos CBC — Espingarda
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES ('Momentum'), ('Military 3.0'), ('Montenegro')) AS m(name)
         CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'CBC') b
         CROSS JOIN (SELECT id FROM weapon_types WHERE name = 'Espingarda') t;

-- Modelos Smith & Wesson — Revólver
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES ('Model 686'), ('Model 10')) AS m(name)
         CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'Smith & Wesson') b
         CROSS JOIN (SELECT id FROM weapon_types WHERE name = 'Revólver') t;

-- Modelos Smith & Wesson — Pistola
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES ('M&P9'), ('M&P Shield')) AS m(name)
         CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'Smith & Wesson') b
         CROSS JOIN (SELECT id FROM weapon_types WHERE name = 'Pistola') t;

-- Helper: associa modelo(s) a calibre(s) por nome
-- weapon_model_calibers (modelo, marca, calibre)
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
         JOIN weapon_calibers wc ON wc.name = combo.caliber_name;