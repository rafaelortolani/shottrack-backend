-- V58: 7 modelos que ficaram pendentes na V57 e foram confirmados (fonte: 10 listagens de loja, nov/2026).
-- Somente ACRESCENTA (marcas, tipo, calibres, modelos e relações modelo-calibre). Não apaga nem altera nada existente.
-- Idempotente: compara por nome sem diferenciar maiúsculas/minúsculas; rodar de novo não duplica.
-- Termina com uma conferência: se algo esperado não existir (ou um modelo existente tiver tipo diferente), a migration falha
-- e nada é gravado (Flyway roda em transação no PostgreSQL).

CREATE TEMP TABLE tmp_catalog (brand text, type text, model text, caliber text) ON COMMIT DROP;

INSERT INTO tmp_catalog (brand, type, model, caliber) VALUES
  ('Alfa Proj', 'Pistola', 'Brno CZ 3035', '6.35mm'),
  ('Taurus', 'Pistola', '59', '.380 ACP'),
  ('Taurus', 'Pistola', 'PT59S', '.380 ACP'),
  ('Taurus', 'Pistola', 'TH', '.45 ACP'),
  ('Taurus', 'Revólver', '38SA', '.38 Special'),
  ('Taurus', 'Revólver', 'RM66', '.38 Special'),
  ('Taurus', 'Revólver', 'RM66', '.357 Magnum'),
  ('CBC', 'Rifle', 'Ranger Pro', '.308 Winchester');

-- Tipos novos (hoje só falta "Rifle")
INSERT INTO weapon_types (name)
SELECT DISTINCT t.type FROM tmp_catalog t
WHERE NOT EXISTS (SELECT 1 FROM weapon_types x WHERE lower(x.name) = lower(t.type));

-- Calibres novos
INSERT INTO weapon_calibers (name)
SELECT DISTINCT t.caliber FROM tmp_catalog t
WHERE NOT EXISTS (SELECT 1 FROM weapon_calibers x WHERE lower(x.name) = lower(t.caliber));

-- Marcas novas
INSERT INTO weapon_brands (name)
SELECT DISTINCT t.brand FROM tmp_catalog t
WHERE NOT EXISTS (SELECT 1 FROM weapon_brands x WHERE lower(x.name) = lower(t.brand));

-- Modelos novos (marca + nome não existentes)
INSERT INTO weapon_models (brand_id, weapon_type_id, name)
SELECT DISTINCT b.id, ty.id, t.model
FROM tmp_catalog t
JOIN weapon_brands b ON lower(b.name) = lower(t.brand)
JOIN weapon_types ty ON lower(ty.name) = lower(t.type)
WHERE NOT EXISTS (
    SELECT 1 FROM weapon_models m
    WHERE m.brand_id = b.id AND lower(m.name) = lower(t.model));

-- Relações modelo-calibre novas (inclui calibres novos em modelos que já existiam)
INSERT INTO weapon_model_calibers (weapon_model_id, weapon_caliber_id)
SELECT DISTINCT m.id, c.id
FROM tmp_catalog t
JOIN weapon_brands b ON lower(b.name) = lower(t.brand)
JOIN weapon_models m ON m.brand_id = b.id AND lower(m.name) = lower(t.model)
JOIN weapon_calibers c ON lower(c.name) = lower(t.caliber)
WHERE NOT EXISTS (
    SELECT 1 FROM weapon_model_calibers x
    WHERE x.weapon_model_id = m.id AND x.weapon_caliber_id = c.id);

-- Conferência: tudo que a lista pede precisa existir, com o tipo esperado
DO $$
DECLARE
    faltando_relacao integer;
    tipo_diferente   integer;
BEGIN
    SELECT count(*) INTO faltando_relacao
    FROM (SELECT DISTINCT brand, model, caliber FROM tmp_catalog) t
    WHERE NOT EXISTS (
        SELECT 1
        FROM weapon_brands b
        JOIN weapon_models m ON m.brand_id = b.id
        JOIN weapon_model_calibers mc ON mc.weapon_model_id = m.id
        JOIN weapon_calibers c ON c.id = mc.weapon_caliber_id
        WHERE lower(b.name) = lower(t.brand) AND lower(m.name) = lower(t.model)
          AND lower(c.name) = lower(t.caliber));

    SELECT count(*) INTO tipo_diferente
    FROM (SELECT DISTINCT brand, type, model FROM tmp_catalog) t
    JOIN weapon_brands b ON lower(b.name) = lower(t.brand)
    JOIN weapon_models m ON m.brand_id = b.id AND lower(m.name) = lower(t.model)
    JOIN weapon_types ty ON ty.id = m.weapon_type_id
    WHERE lower(ty.name) <> lower(t.type);

    IF faltando_relacao > 0 THEN
        RAISE EXCEPTION 'V58: % relacao(oes) modelo-calibre nao foram criadas', faltando_relacao;
    END IF;
    IF tipo_diferente > 0 THEN
        RAISE EXCEPTION 'V58: % modelo(s) ja existem com tipo diferente do esperado', tipo_diferente;
    END IF;
END $$;
