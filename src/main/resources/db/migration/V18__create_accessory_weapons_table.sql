CREATE TABLE accessory_weapons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accessory_id UUID NOT NULL REFERENCES accessories(id),
    weapon_id UUID NOT NULL REFERENCES weapons(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    UNIQUE (accessory_id, weapon_id)
);
