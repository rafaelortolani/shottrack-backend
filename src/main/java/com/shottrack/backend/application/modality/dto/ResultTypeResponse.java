package com.shottrack.backend.application.modality.dto;

import java.util.UUID;

public record ResultTypeResponse(
        UUID id,
        String name
) {
}
