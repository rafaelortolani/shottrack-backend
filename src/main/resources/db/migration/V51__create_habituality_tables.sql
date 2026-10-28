-- ADR-0023: habitualidade configurável. Nenhum seed: os valores legais são
-- cadastrados pelo administrador. Sem ON DELETE CASCADE: remoção de versão apaga
-- exigências, níveis e a versão, nessa ordem, em código.
CREATE TABLE habituality_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    legal_reference VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE UNIQUE INDEX uq_habituality_rules_name ON habituality_rules(lower(name));

CREATE TABLE habituality_rule_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES habituality_rules(id),
    valid_from DATE NOT NULL,
    window_months INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_habituality_versions_rule_valid_from UNIQUE (rule_id, valid_from),
    CONSTRAINT ck_habituality_versions_window CHECK (window_months >= 1)
);

CREATE TABLE habituality_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL REFERENCES habituality_rule_versions(id),
    name VARCHAR(100) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_habituality_levels_version_name UNIQUE (version_id, name)
);

CREATE TABLE habituality_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level_id UUID NOT NULL REFERENCES habituality_levels(id),
    activity_type VARCHAR(20) NOT NULL,
    min_quantity INTEGER NOT NULL,
    only_in_club BOOLEAN NOT NULL DEFAULT FALSE,
    distinct_events BOOLEAN NOT NULL DEFAULT FALSE,
    min_scope VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ck_habituality_requirements_type CHECK (activity_type IN ('TREINO', 'COMPETICAO', 'QUALQUER')),
    CONSTRAINT ck_habituality_requirements_quantity CHECK (min_quantity >= 1),
    CONSTRAINT ck_habituality_requirements_scope
        CHECK (min_scope IS NULL OR (min_scope IN ('ESTADUAL', 'NACIONAL', 'INTERNACIONAL') AND activity_type <> 'TREINO'))
);

CREATE TABLE athlete_habituality_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    rule_id UUID NOT NULL REFERENCES habituality_rules(id),
    level_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_athlete_habituality_tracking_user UNIQUE (user_id)
);
CREATE INDEX idx_athlete_habituality_tracking_rule_id ON athlete_habituality_tracking(rule_id);

CREATE TABLE athlete_personal_goal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    yearly_trainings INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_athlete_personal_goal_user UNIQUE (user_id),
    CONSTRAINT ck_athlete_personal_goal_range CHECK (yearly_trainings BETWEEN 1 AND 1000)
);
CREATE INDEX idx_habituality_versions_rule_id ON habituality_rule_versions(rule_id);
CREATE INDEX idx_habituality_levels_version_id ON habituality_levels(version_id);
CREATE INDEX idx_habituality_requirements_level_id ON habituality_requirements(level_id);
