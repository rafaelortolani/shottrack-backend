-- ADR-0016: o destaque geral único vira lista de recordes — melhor valor
-- de cada tipo MENOR_MELHOR/MAIOR_MELHOR com registro. Mesmo padrão das
-- outras coleções do resumo (V37/V40).
CREATE TABLE dashboard_summary_records (
    dashboard_summary_id UUID NOT NULL REFERENCES dashboard_summary(id) ON DELETE CASCADE,
    result_type_name VARCHAR(50) NOT NULL,
    best_value NUMERIC NOT NULL
);

ALTER TABLE dashboard_summary
    DROP COLUMN highlight_result_type_name,
    DROP COLUMN highlight_value;

-- ADR-0015: resumo descartável — os antigos não têm recordes; o UC42 cai
-- no fallback até o próximo evento regravar.
DELETE FROM dashboard_summary;
