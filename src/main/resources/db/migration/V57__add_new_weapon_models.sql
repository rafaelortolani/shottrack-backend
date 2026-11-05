-- V57: novos modelos de armas (fonte: 10 listagens de loja, nov/2026) — 119 modelos.
-- Somente ACRESCENTA (marcas, tipo, calibres, modelos e relações modelo-calibre). Não apaga nem altera nada existente.
-- Idempotente: compara por nome sem diferenciar maiúsculas/minúsculas; rodar de novo não duplica.
-- Termina com uma conferência: se algo esperado não existir (ou um modelo existente tiver tipo diferente), a migration falha
-- e nada é gravado (Flyway roda em transação no PostgreSQL).

CREATE TEMP TABLE tmp_catalog (brand text, type text, model text, caliber text) ON COMMIT DROP;

INSERT INTO tmp_catalog (brand, type, model, caliber) VALUES
  ('Taurus', 'Pistola', 'G2C', '.38 TPC'),
  ('Taurus', 'Pistola', 'G2C', '9mm'),
  ('Taurus', 'Pistola', 'G2C', '.40 S&W'),
  ('Taurus', 'Pistola', 'GX2', '.38 TPC'),
  ('Taurus', 'Pistola', 'GX2', '9mm'),
  ('Taurus', 'Pistola', 'G3', '.38 TPC'),
  ('Taurus', 'Pistola', 'G3', '9mm'),
  ('Taurus', 'Pistola', 'G3 Tactical', '9mm'),
  ('Taurus', 'Pistola', 'GX4', '9mm'),
  ('Taurus', 'Pistola', 'GX4 Carry', '.380 ACP'),
  ('Taurus', 'Pistola', 'GX4 Carry', '.38 TPC'),
  ('Taurus', 'Pistola', 'GX4 Carry', '9mm'),
  ('Taurus', 'Pistola', 'GX4 Micro', '.38 TPC'),
  ('Taurus', 'Pistola', 'TX22', '.22 LR'),
  ('Taurus', 'Pistola', 'TH380', '.380 ACP'),
  ('Taurus', 'Pistola', 'TH380C', '.380 ACP'),
  ('Taurus', 'Pistola', 'TX Compacta', '.38 TPC'),
  ('Taurus', 'Pistola', 'TX Full Size', '.38 TPC'),
  ('Taurus', 'Pistola', 'TX Subcompacta', '.38 TPC'),
  ('Taurus', 'Pistola', 'TX9 Full Size', '9mm'),
  ('Taurus', 'Pistola', 'TX9 Compacta', '9mm'),
  ('Taurus', 'Pistola', '1911 Officer', '.38 TPC'),
  ('Taurus', 'Pistola', '1911 Officer', '.45 ACP'),
  ('Taurus', 'Pistola', '1911 Commander', '.38 TPC'),
  ('Taurus', 'Pistola', '1911 Commander', '.45 ACP'),
  ('Taurus', 'Pistola', '1911 Government', '.38 TPC'),
  ('Taurus', 'Pistola', '1911 Government', '9mm'),
  ('Taurus', 'Pistola', '1911 Clássica', '.45 ACP'),
  ('Taurus', 'Pistola', '58HC Plus', '.380 ACP'),
  ('Taurus', 'Pistola', 'TH9C', '9mm'),
  ('Taurus', 'Pistola', 'TS9C', '9mm'),
  ('Taurus', 'Pistola', 'PT 917', '9mm'),
  ('Taurus', 'Pistola', 'PT 92', '9mm'),
  ('Bersa', 'Pistola', 'Thunder Plus', '.380 ACP'),
  ('CZ', 'Pistola', 'P-10 C', '.380 ACP'),
  ('CZ', 'Pistola', 'P-10 C', '9mm'),
  ('CZ', 'Pistola', 'P-10 M', '9mm'),
  ('CZ', 'Pistola', 'P-10 S', '9mm'),
  ('CZ', 'Pistola', 'P-09', '9mm'),
  ('CZ', 'Pistola', 'P-07', '.380 ACP'),
  ('CZ', 'Pistola', 'Shadow 2', '.380 ACP'),
  ('CZ', 'Pistola', 'Shadow 2', '9mm'),
  ('Glock', 'Pistola', 'G25', '.380 ACP'),
  ('Springfield', 'Pistola', 'Hellcat OSP', '.380 ACP'),
  ('Smith & Wesson', 'Pistola', 'Bodyguard 2.0', '.380 ACP'),
  ('Smith & Wesson', 'Pistola', 'SD9', '9mm'),
  ('Smith & Wesson', 'Pistola', 'M&P9 Shield Plus', '9mm'),
  ('Smith & Wesson', 'Pistola', 'M&P9 M2.0', '9mm'),
  ('Sig Sauer', 'Pistola', 'P322', '.22 LR'),
  ('Sig Sauer', 'Pistola', 'P365', '.380 ACP'),
  ('Beretta', 'Pistola', '80X Guepardo', '.380 ACP'),
  ('Beretta', 'Pistola', 'M9A4', '9mm'),
  ('Beretta', 'Pistola', '94X Performance', '9mm'),
  ('Arex', 'Pistola', 'Delta Gen.2 M', '9mm'),
  ('Ruger', 'Pistola', 'Security-9', '9mm'),
  ('Ruger', 'Revólver', 'Wrangler', '.22 LR'),
  ('Taurus', 'Revólver', '66', '.38 Special'),
  ('Taurus', 'Revólver', '66', '.357 Magnum'),
  ('Taurus', 'Revólver', '85', '.38 Special'),
  ('Taurus', 'Revólver', '82', '.38 Special'),
  ('Taurus', 'Revólver', '889', '.38 Special'),
  ('Taurus', 'Revólver', '838', '.38 Special'),
  ('Taurus', 'Revólver', '380', '.380 ACP'),
  ('Taurus', 'Revólver', '96', '.22 LR'),
  ('Taurus', 'Revólver', '88', '.38 Special'),
  ('Taurus', 'Revólver', '817', '.38 Special'),
  ('Taurus', 'Revólver', '832 Tracker', '.38 Special'),
  ('Taurus', 'Revólver', '832 Tracker', '.380 ACP'),
  ('Taurus', 'Revólver', '856', '.38 Special'),
  ('Taurus', 'Revólver', '970', '.22 LR'),
  ('Taurus', 'Revólver', 'Judge', '.410'),
  ('Taurus', 'Revólver', 'Judge', '.45 Colt'),
  ('Taurus', 'Revólver', 'Raging Hunter', '.38 Special'),
  ('Taurus', 'Revólver', 'Raging Hunter', '.44 Magnum'),
  ('Taurus', 'Revólver', 'Raging Hunter', '.500 S&W Magnum'),
  ('Taurus', 'Revólver', '627', '.357 Magnum'),
  ('Taurus', 'Revólver', '608', '.357 Magnum'),
  ('Taurus', 'Revólver', 'RP63', '.357 Magnum'),
  ('Taurus', 'Revólver', 'RP63', '.38 Special'),
  ('Taurus', 'Revólver', '992', '.22 LR'),
  ('Taurus', 'Revólver', '992', '.22 WMR'),
  ('Taurus', 'Revólver', '044', '.44 Magnum'),
  ('Taurus', 'Revólver', '444 Raging Bull', '.44 Magnum'),
  ('Taurus', 'Revólver', '454 Raging Bull', '.454 Casull'),
  ('Taurus', 'Revólver', '44H', '.44 Magnum'),
  ('Rossi', 'Revólver', 'RM64', '.38 Special'),
  ('Rossi', 'Revólver', 'RM66', '.38 Special'),
  ('Taurus', 'Carabina', 'CT9', '9mm'),
  ('Taurus', 'Carabina', 'CTT40', '.40 S&W'),
  ('Taurus', 'Carabina', 'BM-F-9 Brigade', '9mm'),
  ('Taurus', 'Carabina', 'RPC', '9mm'),
  ('Ruger', 'Carabina', 'PC Charger', '9mm'),
  ('Ruger', 'Carabina', 'PC Carbine', '9mm'),
  ('Fire Eagle', 'Carabina', 'FE-905PR', '9mm'),
  ('Fire Eagle', 'Carabina', 'FE-908PR', '9mm'),
  ('Fire Eagle', 'Carabina', 'FE-914PR', '9mm'),
  ('Fire Eagle', 'Carabina', 'FE-411S', '.40 S&W'),
  ('CZ', 'Carabina', 'Scorpion EVO 3 S1', '9mm'),
  ('Fire Eagle', 'Fuzil', 'FE-115S', '5.56mm NATO'),
  ('Fire Eagle', 'Fuzil', 'FE-145S', '5.56mm NATO'),
  ('Fire Eagle', 'Fuzil', 'FE-160S', '5.56mm NATO'),
  ('Fire Eagle', 'Fuzil', 'FE-200S', '5.56mm NATO'),
  ('Fire Eagle', 'Fuzil', 'FE-115SGP', '5.56mm NATO'),
  ('Fire Eagle', 'Fuzil', 'FE-145SGP', '5.56mm NATO'),
  ('Fire Eagle', 'Fuzil', 'FE-307S', '.300 BLK'),
  ('Fire Eagle', 'Fuzil', 'FE-310S', '.300 BLK'),
  ('Fire Eagle', 'Fuzil', 'Dark 300 CQB', '.300 BLK'),
  ('Fire Eagle', 'Fuzil', 'Dark 300 LE', '.300 BLK'),
  ('Taurus', 'Fuzil', 'T4', '5.56mm NATO'),
  ('Taurus', 'Fuzil', 'T4 MLOK', '5.56mm NATO'),
  ('Taurus', 'Fuzil', 'T4 Executive Grade', '5.56mm NATO'),
  ('Taurus', 'Fuzil', 'T4 Executive Grade', '.223 Remington'),
  ('Taurus', 'Fuzil', 'T4 Executive Grade', '.300 BLK'),
  ('Taurus', 'Fuzil', 'T10', '.308 Winchester'),
  ('Taurus', 'Fuzil', 'T10 LP', '.308 Winchester'),
  ('Mossberg', 'Fuzil', 'MMR Hunter', '5.56mm NATO'),
  ('CBC', 'Rifle', '8122', '.22 LR'),
  ('CBC', 'Rifle', '8122M', '.22 WMR'),
  ('CBC', 'Rifle', 'Pump Action', '.22 LR'),
  ('CBC', 'Rifle', 'Rio Bravo', '.22 LR'),
  ('CBC', 'Rifle', 'Delta', '.22 LR'),
  ('Browning', 'Rifle', 'T-Bolt Sporter', '.22 LR'),
  ('Smith & Wesson', 'Rifle', 'MP15-22', '.22 LR'),
  ('CBC', 'Rifle', 'Ranger', '.308 Winchester'),
  ('Mossberg', 'Rifle', 'Patriot', '7mm Remington Magnum'),
  ('Savage', 'Rifle', 'Axis II Overwatch', '.223 Remington'),
  ('Boito', 'Espingarda', 'B-300', '28'),
  ('Boito', 'Espingarda', 'B-300', '32'),
  ('Boito', 'Espingarda', 'B-300', '.410'),
  ('Boito', 'Espingarda', 'Reuna', '12'),
  ('Boito', 'Espingarda', 'Reuna', '20'),
  ('Boito', 'Espingarda', 'Reuna', '28'),
  ('Boito', 'Espingarda', 'Reuna', '32'),
  ('Boito', 'Espingarda', 'Reuna', '.410'),
  ('Boito', 'Espingarda', 'A-680', '12'),
  ('Boito', 'Espingarda', 'A-680', '20'),
  ('Boito', 'Espingarda', 'A-680', '28'),
  ('Boito', 'Espingarda', 'A-680', '32'),
  ('Boito', 'Espingarda', 'A-680', '.410'),
  ('Boito', 'Espingarda', 'Pump', '12'),
  ('Boito', 'Espingarda', 'Miúra I', '12'),
  ('Boito', 'Espingarda', 'Miúra I', '20'),
  ('Boito', 'Espingarda', 'Miúra I', '28'),
  ('Boito', 'Espingarda', 'Miúra I', '.410'),
  ('Boito', 'Espingarda', 'Miúra II', '12'),
  ('Boito', 'Espingarda', 'Miúra II', '20'),
  ('Boito', 'Espingarda', 'Miúra II', '28'),
  ('Boito', 'Espingarda', 'Miúra II', '.410'),
  ('Boito', 'Espingarda', 'ERA 2001', '12'),
  ('CBC', 'Espingarda', 'Military 3.0', '12'),
  ('Hatsan', 'Espingarda', 'Escort', '12'),
  ('Kral', 'Espingarda', 'Azarax', '20'),
  ('Kral', 'Espingarda', 'Azarax E200', '20'),
  ('Khan', 'Espingarda', 'Matrix Fidelio', '12'),
  ('Armsan', 'Espingarda', 'Phenoma S', '12');

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
        RAISE EXCEPTION 'V57: % relacao(oes) modelo-calibre nao foram criadas', faltando_relacao;
    END IF;
    IF tipo_diferente > 0 THEN
        RAISE EXCEPTION 'V57: % modelo(s) ja existem com tipo diferente do esperado', tipo_diferente;
    END IF;
END $$;
