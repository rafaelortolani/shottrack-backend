CREATE TABLE series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_id UUID NOT NULL REFERENCES trainings(id),
    weapon_id UUID REFERENCES weapons(id),
    ammunition_id UUID REFERENCES ammunitions(id),
    distance_meters NUMERIC,
    target TEXT,
    shot_count INTEGER,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
