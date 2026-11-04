-- ADR-0026 (etapa A): foto do alvo anexada à série. O arquivo fica no armazenamento (StoragePort); aqui só os
-- metadados. As linhas somem junto com a série (cascata); os arquivos são apagados pela aplicação, após o commit.
CREATE TABLE series_photos (
    id UUID PRIMARY KEY,
    series_id UUID NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    athlete_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_key VARCHAR(255) NOT NULL,
    thumb_key VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    width_px INTEGER NOT NULL,
    height_px INTEGER NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_series_photos_series_id ON series_photos (series_id);
CREATE INDEX idx_series_photos_athlete_id ON series_photos (athlete_id);
