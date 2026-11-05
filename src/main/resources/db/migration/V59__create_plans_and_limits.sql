-- ADR-0027: planos e limites administráveis. As chaves de limite são um enum no código; aqui ficam só os valores.
-- Chave sem valor em plan_limits usa o padrão do enum. Planos nunca são excluídos (só desativados).
CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(60) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    -- o plano padrão precisa estar ativo
    CONSTRAINT ck_plans_default_is_active CHECK (NOT is_default OR active)
);

-- código e nome únicos sem diferenciar maiúsculas/minúsculas
CREATE UNIQUE INDEX uq_plans_code ON plans (lower(code));
CREATE UNIQUE INDEX uq_plans_name ON plans (lower(name));
-- no máximo 1 plano padrão (o "pelo menos 1" é garantido pela aplicação e conferido na subida)
CREATE UNIQUE INDEX uq_plans_single_default ON plans (is_default) WHERE is_default;

CREATE TABLE plan_limits (
    plan_id UUID NOT NULL REFERENCES plans(id),
    limit_key VARCHAR(50) NOT NULL,
    value BIGINT NOT NULL,
    PRIMARY KEY (plan_id, limit_key)
);

INSERT INTO plans (code, name, active, is_default) VALUES ('FREE', 'Gratuito', TRUE, TRUE);

INSERT INTO plan_limits (plan_id, limit_key, value)
SELECT p.id, v.limit_key, v.value
FROM plans p,
     (VALUES ('PHOTOS_PER_SERIES', 5::BIGINT),
             ('PHOTOS_PER_ATHLETE', 500::BIGINT),
             ('STORAGE_BYTES_PER_ATHLETE', 536870912::BIGINT),
             ('UPLOADS_PER_HOUR', 30::BIGINT)) AS v(limit_key, value)
WHERE p.code = 'FREE';

-- todo atleta tem plano: anulável, preenche todos com o Gratuito, depois NOT NULL
ALTER TABLE users ADD COLUMN plan_id UUID REFERENCES plans(id);
UPDATE users SET plan_id = (SELECT id FROM plans WHERE code = 'FREE');
ALTER TABLE users ALTER COLUMN plan_id SET NOT NULL;
CREATE INDEX idx_users_plan_id ON users (plan_id);

-- Auditoria das ações de administrador sobre planos (ADR-0021 só prevê as colunas de auditoria das tabelas;
-- não havia registro de ações). Ator e alvo-usuário viram NULL quando a conta é excluída (UC52): o registro fica.
CREATE TABLE plan_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    at TIMESTAMP NOT NULL DEFAULT now(),
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(20) NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    target_plan_code VARCHAR(30),
    target_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    -- chave do limite (LIMIT_CHANGED) ou campo do plano (PLAN_UPDATED: name, active, default)
    field_key VARCHAR(50),
    old_value VARCHAR(100),
    new_value VARCHAR(100),
    CONSTRAINT ck_plan_audit_action CHECK (action IN ('LIMIT_CHANGED', 'PLAN_ASSIGNED', 'PLAN_CREATED', 'PLAN_UPDATED')),
    CONSTRAINT ck_plan_audit_target CHECK (target_type IN ('PLAN', 'USER'))
);

CREATE INDEX idx_plan_audit_at ON plan_audit (at DESC);
CREATE INDEX idx_plan_audit_actor ON plan_audit (actor_user_id);
CREATE INDEX idx_plan_audit_target_user ON plan_audit (target_user_id);
