ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID;
