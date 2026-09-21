-- Sugestão padrão por modalidade (ADR-0011) — aplicada automaticamente
-- como seleção inicial do atleta quando ele adiciona a modalidade (UC12).
INSERT INTO modality_result_type_suggestions (modality_id, result_type_id)
SELECT m.id, rt.id
FROM (VALUES
    ('Precisão', 'Pontuação'),
    ('Precisão', 'Agrupamento'),
    ('IPSC', 'Tempo'),
    ('IPSC', 'Pontuação'),
    ('IPSC', 'Fator de desempenho'),
    ('Steel Challenge', 'Tempo'),
    ('Trap', 'Acertos'),
    ('Trap', 'Erros'),
    ('Skeet', 'Acertos'),
    ('Skeet', 'Erros'),
    ('Saque e Tiro', 'Tempo'),
    ('Saque e Tiro', 'Acertos')
) AS suggestion(modality_name, result_type_name)
JOIN modalities m ON m.name = suggestion.modality_name
JOIN result_types rt ON rt.name = suggestion.result_type_name;
