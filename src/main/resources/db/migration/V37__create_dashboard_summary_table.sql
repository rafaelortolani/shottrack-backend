-- ADR-0015: um registro por atleta, recalculado do zero a cada evento
-- DashboardRecalculationRequested. modalidades praticadas fica numa
-- tabela auxiliar (owned inteiramente por dashboard_summary, sem coluna
-- própria de auditoria/id) em vez de campo simples — evita depender de
-- um formato de serialização pra lista dentro de uma coluna.
CREATE TABLE dashboard_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    trainings_this_month INTEGER NOT NULL,
    shots_this_month INTEGER NOT NULL,
    highlight_result_type_name VARCHAR(50),
    highlight_value NUMERIC,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE dashboard_summary_modalities (
    dashboard_summary_id UUID NOT NULL REFERENCES dashboard_summary(id) ON DELETE CASCADE,
    modality_name VARCHAR(50) NOT NULL
);
