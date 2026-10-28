-- ADR-0023: tipo da visita (treino ou competição), âmbito (só competição) e marca
-- "é clube" no local de treino. Registros existentes ficam TREINO, sem âmbito, e
-- locais ficam "não é clube".
ALTER TABLE visits ADD COLUMN visit_type VARCHAR(20) NOT NULL DEFAULT 'TREINO';
ALTER TABLE visits ADD COLUMN competition_scope VARCHAR(20);
ALTER TABLE visits ADD CONSTRAINT ck_visits_visit_type CHECK (visit_type IN ('TREINO', 'COMPETICAO'));
ALTER TABLE visits ADD CONSTRAINT ck_visits_competition_scope
    CHECK (competition_scope IS NULL OR competition_scope IN ('ESTADUAL', 'NACIONAL', 'INTERNACIONAL'));
ALTER TABLE visits ADD CONSTRAINT ck_visits_scope_only_competition
    CHECK (competition_scope IS NULL OR visit_type = 'COMPETICAO');

ALTER TABLE training_locations ADD COLUMN club BOOLEAN NOT NULL DEFAULT FALSE;

-- Apuração de habitualidade (UC57/UC58): visitas encerradas do atleta numa janela de datas.
CREATE INDEX idx_visits_user_status_created ON visits(user_id, status, created_at);
