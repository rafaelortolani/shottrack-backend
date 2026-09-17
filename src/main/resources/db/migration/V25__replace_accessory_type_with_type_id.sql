-- Breaking change aceita em dev (ADR-0008, revisão): tipo passa de texto
-- livre pra catálogo fechado, sem correspondência garantida entre os dois.
-- Acessórios já cadastrados são descartados (cascata em accessory_weapons).
DELETE FROM accessories;

ALTER TABLE accessories DROP COLUMN type;
ALTER TABLE accessories ADD COLUMN type_id UUID NOT NULL REFERENCES accessory_types(id);
