package com.shottrack.backend.application.accessory.dto;

import java.time.Instant;
import java.util.UUID;

public record AccessoryResponse(
        UUID id,
        String name,
        String type,
        String notes,
        Instant createdAt
) {
}
