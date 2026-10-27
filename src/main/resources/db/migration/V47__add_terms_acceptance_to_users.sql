-- ADR-0019 (UC23): aceite dos Termos de Uso e da Política de Privacidade.
-- Nulo para contas anteriores ao aceite (só existem contas de teste).
ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN terms_version VARCHAR(20);
