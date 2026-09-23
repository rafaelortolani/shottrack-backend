-- UC42 (Onda 1): resumo por modalidade — contagem de treinos e melhor
-- valor do tipo mais registrado naquela modalidade. Mesmo padrão de
-- dashboard_summary_modalities (V37): tabela auxiliar owned pelo resumo.
CREATE TABLE dashboard_summary_modality_stats (
    dashboard_summary_id UUID NOT NULL REFERENCES dashboard_summary(id) ON DELETE CASCADE,
    modality_name VARCHAR(50) NOT NULL,
    training_count INTEGER NOT NULL,
    best_result_type_name VARCHAR(50),
    best_value NUMERIC
);

-- ADR-0015: o resumo é descartável (sempre recalculado do zero). Resumos
-- antigos não têm as estatísticas por modalidade — apagando, o UC42 cai
-- no fallback (cálculo na hora) até o próximo evento regravar o resumo.
DELETE FROM dashboard_summary;
