-- UC42 (onboarding "Criar perfil"): experience_level tem default BEGINNER
-- (V10), então não dá pra saber pelo nível se o atleta já configurou o
-- perfil. Marca a primeira edição de perfil (UC04); nulo = pendente.
ALTER TABLE users ADD COLUMN profile_completed_at TIMESTAMP;
