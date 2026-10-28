-- ADR-0022 (UC42 v7): o dashboard passa a filtrar por modalidade e a comparar com
-- o período anterior.
-- Indicadores do mês (treinos, disparos, modalidades) dependem da data de hoje
-- (janela móvel) e passam a ser calculados sob demanda — saem do resumo
-- pré-calculado. O resumo guarda só o que depende do histórico inteiro:
-- recordes e estatísticas por modalidade (agora com id e recordes por modalidade,
-- que o filtro e a evolução sugerida precisam).

-- ADR-0015: o resumo é descartável (sempre recalculado do zero). Os antigos não têm
-- modality_id nem recordes por modalidade; apagando, o UC42 cai no fallback (cálculo
-- na hora) até o próximo evento regravar o resumo. Filhas primeiro (sem depender
-- de cascata).
DELETE FROM dashboard_summary_records;
DELETE FROM dashboard_summary_modality_stats;
DELETE FROM dashboard_summary_modalities;
DELETE FROM dashboard_summary;

ALTER TABLE dashboard_summary
    DROP COLUMN trainings_this_month,
    DROP COLUMN shots_this_month;

DROP TABLE dashboard_summary_modalities;

ALTER TABLE dashboard_summary_modality_stats ADD COLUMN modality_id UUID NOT NULL;

CREATE TABLE dashboard_summary_modality_records (
    dashboard_summary_id UUID NOT NULL REFERENCES dashboard_summary(id),
    modality_id UUID NOT NULL,
    result_type_id UUID NOT NULL,
    result_type_name VARCHAR(50) NOT NULL,
    best_value NUMERIC NOT NULL,
    record_count INTEGER NOT NULL
);

-- Contagens sob demanda: visits (user_id) → trainings (visit_id) → series (training_id).
CREATE INDEX idx_visits_user_id ON visits(user_id);
CREATE INDEX idx_trainings_visit_id ON trainings(visit_id);
CREATE INDEX idx_series_training_id ON series(training_id);
