package com.shottrack.backend.application.traininglocation.dto;

import java.time.Instant;
import java.util.UUID;

public record TrainingLocationResponse(
        UUID id,
        String name,
        String city,
        String state,
        Instant createdAt
) {
}
