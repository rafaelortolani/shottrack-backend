INSERT INTO weapon_types (name) VALUES
    ('Pistola'),
    ('Revólver'),
    ('Carabina'),
    ('Espingarda'),
    ('Outro');

INSERT INTO weapon_brands (name) VALUES
    ('Taurus'),
    ('Glock'),
    ('Imbel'),
    ('CBC'),
    ('Rossi');

INSERT INTO weapon_models (brand_id, name)
SELECT id, model_name
FROM weapon_brands
JOIN (VALUES
    ('Taurus', 'G2C'),
    ('Taurus', '856'),
    ('Glock', 'G17'),
    ('Glock', 'G19'),
    ('Imbel', 'IA2'),
    ('Imbel', 'MD2'),
    ('CBC', 'Momentum'),
    ('Rossi', 'RS22')
) AS models(brand_name, model_name) ON weapon_brands.name = models.brand_name;

INSERT INTO weapon_calibers (name) VALUES
    ('.22 LR'),
    ('.38 SPL'),
    ('9mm'),
    ('.40 S&W'),
    ('.45 ACP'),
    ('12 Gauge'),
    ('.223/5.56');
