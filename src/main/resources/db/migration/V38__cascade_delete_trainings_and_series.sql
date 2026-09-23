-- UC43/UC44: excluir treino remove suas séries; excluir visita remove seus
-- treinos (e, em cadeia, as séries deles). Os resultados de cada série já
-- somem via series_results_series_id_fkey (V34).
ALTER TABLE trainings
    DROP CONSTRAINT trainings_visit_id_fkey,
    ADD CONSTRAINT trainings_visit_id_fkey
        FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE CASCADE;

ALTER TABLE series
    DROP CONSTRAINT series_training_id_fkey,
    ADD CONSTRAINT series_training_id_fkey
        FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE;
