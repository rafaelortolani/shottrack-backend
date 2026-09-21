CREATE TABLE modality_result_type_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modality_id UUID NOT NULL REFERENCES modalities(id),
    result_type_id UUID NOT NULL REFERENCES result_types(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    UNIQUE (modality_id, result_type_id)
);
