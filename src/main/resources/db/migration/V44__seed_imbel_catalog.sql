-- ADR-0004 (Revisão 2): linha Imbel com modelos, tipos e calibres reais.
-- Nome do modelo sem o prefixo "IMBEL" (a marca vem do vínculo, como nos
-- outros modelos). O IA2 existe em mais de uma versão, e cada modelo tem um
-- único tipo — então cada versão é um modelo próprio, com o tipo no nome.
-- 9×19 mm e 5,56×45 mm reaproveitam os calibres '9mm' e '5.56mm NATO'.

-- Os antigos 'IA2' e 'MD2' (V43) saem: o MD2 real é pistola .380 (GC MD2),
-- e o IA2 passa a ter uma entrada por versão. Só se nenhuma arma os usa.
DELETE FROM weapon_model_calibers
WHERE weapon_model_id IN (
    SELECT m.id FROM weapon_models m
    JOIN weapon_brands b ON b.id = m.brand_id
    WHERE b.name = 'Imbel' AND m.name IN ('IA2', 'MD2')
      AND NOT EXISTS (SELECT 1 FROM weapons WHERE weapons.model_id = m.id));

DELETE FROM weapon_models
WHERE name IN ('IA2', 'MD2')
  AND brand_id = (SELECT id FROM weapon_brands WHERE name = 'Imbel')
  AND NOT EXISTS (SELECT 1 FROM weapons WHERE weapons.model_id = weapon_models.id);

-- Tipos
INSERT INTO weapon_types (name) VALUES
    ('Fuzil'), ('Fuzil de precisão'), ('Fuzil de treinamento')
ON CONFLICT (name) DO NOTHING;

-- Calibres
INSERT INTO weapon_calibers (name) VALUES
    ('.380 ACP'), ('7.62mm NATO'), ('.308 Winchester')
ON CONFLICT (name) DO NOTHING;

-- Modelos
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT b.id, t.id, m.name
FROM (VALUES
    ('.380 GC MD1', 'Pistola'),
    ('.380 GC MD2', 'Pistola'),
    ('.40 GC MD7', 'Pistola'),
    ('.40 TC MD6', 'Pistola'),
    ('9 GC MD1', 'Pistola'),
    ('9 TC MD6', 'Pistola'),
    ('5,56 IA2 Carabina', 'Carabina'),
    ('7,62 IA2 Carabina', 'Carabina'),
    ('5,56 IA2 Fuzil', 'Fuzil'),
    ('7,62 IA2 Fuzil', 'Fuzil'),
    ('M964A1 MD1 – PARAFAL', 'Fuzil'),
    ('308 ISR-100/18', 'Fuzil de precisão'),
    ('5,56 IA2 Treinamento', 'Fuzil de treinamento')
) AS m(name, type_name)
CROSS JOIN (SELECT id FROM weapon_brands WHERE name = 'Imbel') b
JOIN weapon_types t ON t.name = m.type_name
ON CONFLICT (brand_id, name) DO UPDATE SET weapon_type_id = EXCLUDED.weapon_type_id;

-- Calibres válidos de cada modelo
INSERT INTO weapon_model_calibers (weapon_model_id, weapon_caliber_id)
SELECT wm.id, wc.id
FROM (VALUES
    ('.380 GC MD1', '.380 ACP'),
    ('.380 GC MD2', '.380 ACP'),
    ('.40 GC MD7', '.40 S&W'),
    ('.40 TC MD6', '.40 S&W'),
    ('9 GC MD1', '9mm'),
    ('9 TC MD6', '9mm'),
    ('5,56 IA2 Carabina', '5.56mm NATO'),
    ('7,62 IA2 Carabina', '7.62mm NATO'),
    ('5,56 IA2 Fuzil', '5.56mm NATO'),
    ('7,62 IA2 Fuzil', '7.62mm NATO'),
    ('M964A1 MD1 – PARAFAL', '7.62mm NATO'),
    ('308 ISR-100/18', '.308 Winchester'),
    ('5,56 IA2 Treinamento', '5.56mm NATO')
) AS combo(model_name, caliber_name)
JOIN weapon_brands wb ON wb.name = 'Imbel'
JOIN weapon_models wm ON wm.brand_id = wb.id AND wm.name = combo.model_name
JOIN weapon_calibers wc ON wc.name = combo.caliber_name
ON CONFLICT DO NOTHING;
