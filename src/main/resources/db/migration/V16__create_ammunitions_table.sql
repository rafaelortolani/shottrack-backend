CREATE TABLE ammunitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    manufacturer_id UUID REFERENCES ammunition_manufacturers(id),
    caliber_id UUID REFERENCES weapon_calibers(id),
    nickname VARCHAR(100),
    projectile_weight_grains NUMERIC(6,2),
    powder_charge NUMERIC(6,2),
    projectile_type VARCHAR(50),
    lot VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
