-- ADR-0023 (revisado): as regras de habitualidade VIGENTES passam a ser carregadas por
-- migration; o administrador mantém as novas versões (UC55).
--
-- 1) Limpeza: remove as regras criadas para teste e tudo que depende delas — filhos
--    primeiro, sem cascata. Acompanhamentos de atletas que apontam para elas também saem
--    (o atleta precisa escolher regra e nível de novo). Metas pessoais, visitas e locais
--    não são tocados.
DELETE FROM athlete_habituality_tracking;
DELETE FROM habituality_requirements;
DELETE FROM habituality_levels;
DELETE FROM habituality_rule_versions;
DELETE FROM habituality_rules;

-- 2) Regra em vigor.
-- ATENÇÃO: valores extraídos do Decreto 11.615/2023, art. 35, e da Portaria COLOG/C Ex
-- 166/2023, art. 95, e confirmados em duas fontes secundárias. FALTA CONFERIR NO TEXTO
-- OFICIAL. valid_from = início de vigência do decreto; o admin pode ajustar (UC55).
-- As exigências (b) e (c) de cada nível são cumulativas e a (c) está contida na (b): cada
-- uma é contada isoladamente, o que representa "N competições, sendo ao menos 2 de âmbito X".
-- O âmbito mínimo é hierárquico (ESTADUAL < NACIONAL < INTERNACIONAL): ESTADUAL cobre o
-- Nível II (estadual, distrital, regional ou nacional) e NACIONAL cobre o Nível III
-- (nacional ou internacional). Não modelado: "por calibre" e "12 meses de permanência em
-- cada nível". Sem created_by/updated_by, como nos demais seeds (dado de sistema).
DO $$
DECLARE
    v_rule UUID := gen_random_uuid();
    v_version UUID := gen_random_uuid();
    v_level_1 UUID := gen_random_uuid();
    v_level_2 UUID := gen_random_uuid();
    v_level_3 UUID := gen_random_uuid();
BEGIN
    INSERT INTO habituality_rules (id, name, legal_reference, active)
    VALUES (v_rule, 'Habitualidade — Atirador Desportivo',
            'Decreto 11.615/2023, art. 35; Portaria COLOG/C Ex 166/2023, art. 95', TRUE);

    INSERT INTO habituality_rule_versions (id, rule_id, valid_from, window_months)
    VALUES (v_version, v_rule, DATE '2023-12-01', 12);

    INSERT INTO habituality_levels (id, version_id, name, position) VALUES
        (v_level_1, v_version, 'Nível I', 1),
        (v_level_2, v_version, 'Nível II', 2),
        (v_level_3, v_version, 'Nível III', 3);

    INSERT INTO habituality_requirements
        (level_id, activity_type, min_quantity, only_in_club, distinct_events, min_scope) VALUES
        (v_level_1, 'QUALQUER',   8, TRUE,  TRUE,  NULL),
        (v_level_2, 'TREINO',    12, TRUE,  FALSE, NULL),
        (v_level_2, 'COMPETICAO', 4, FALSE, FALSE, NULL),
        (v_level_2, 'COMPETICAO', 2, FALSE, FALSE, 'ESTADUAL'),
        (v_level_3, 'TREINO',    20, TRUE,  FALSE, NULL),
        (v_level_3, 'COMPETICAO', 6, FALSE, FALSE, NULL),
        (v_level_3, 'COMPETICAO', 2, FALSE, FALSE, 'NACIONAL');
END $$;
