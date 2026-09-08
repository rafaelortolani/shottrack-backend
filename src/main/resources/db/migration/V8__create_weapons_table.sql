CREATE TABLE weapons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type_id UUID NOT NULL REFERENCES weapon_types(id),
    brand_id UUID NOT NULL REFERENCES weapon_brands(id),
    model_id UUID NOT NULL REFERENCES weapon_models(id),
    caliber_id UUID NOT NULL REFERENCES weapon_calibers(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
