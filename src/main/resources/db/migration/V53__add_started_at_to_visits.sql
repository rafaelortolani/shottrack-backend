-- ADR-0024: a data de início da visita passa a ser um dado próprio (pode ser retroativa e
-- editada), separado do created_at de auditoria (quando o registro foi lançado). É a data
-- de referência de dashboard, evolução, habitualidade e meta pessoal.
ALTER TABLE visits ADD COLUMN started_at TIMESTAMP;
UPDATE visits SET started_at = created_at;
ALTER TABLE visits ALTER COLUMN started_at SET NOT NULL;

-- As apurações por data agora filtram por started_at.
DROP INDEX idx_visits_user_status_created;
CREATE INDEX idx_visits_user_status_started ON visits(user_id, status, started_at);
CREATE INDEX idx_visits_user_started ON visits(user_id, started_at);
