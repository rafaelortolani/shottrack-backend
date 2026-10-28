package com.shottrack.backend.application.habituality.dto;

import java.time.Instant;
import java.util.UUID;

public record HabitualityRuleResponse(
        UUID id,
        String name,
        String legalReference,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
