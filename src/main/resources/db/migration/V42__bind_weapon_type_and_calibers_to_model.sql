-- ADR-0004 (Revisão 2): tipo e calibres válidos passam a ser do modelo,
-- não escolha solta do atleta no cadastro da arma. Só a estrutura — o
-- seed (tipo e calibres de cada modelo) e o ajuste das armas legadas vêm
-- numa migration separada. weapon_type_id nasce nulo porque os modelos
-- existentes ainda não têm tipo; o NOT NULL entra depois do seed.

ALTER TABLE weapon_models ADD COLUMN weapon_type_id UUID REFERENCES weapon_types(id);

CREATE TABLE weapon_model_calibers (
    weapon_model_id UUID NOT NULL REFERENCES weapon_models(id),
    weapon_caliber_id UUID NOT NULL REFERENCES weapon_calibers(id),
    PRIMARY KEY (weapon_model_id, weapon_caliber_id)
);
