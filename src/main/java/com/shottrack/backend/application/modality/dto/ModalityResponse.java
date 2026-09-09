package com.shottrack.backend.application.modality.dto;

import java.util.UUID;

public record ModalityResponse(
        UUID id,
        String name
) {
}
