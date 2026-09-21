CREATE TABLE visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    training_location_id UUID NOT NULL REFERENCES training_locations(id),
    observations TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
