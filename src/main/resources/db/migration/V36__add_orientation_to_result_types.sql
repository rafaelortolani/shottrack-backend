ALTER TABLE result_types ADD COLUMN orientation VARCHAR(20) NOT NULL DEFAULT 'NAO_APLICAVEL';

UPDATE result_types SET orientation = 'MENOR_MELHOR' WHERE name IN ('Tempo', 'Agrupamento', 'Erros', 'Penalidades');
UPDATE result_types SET orientation = 'MAIOR_MELHOR' WHERE name IN ('Pontuação', 'Acertos', 'Fator de desempenho');
