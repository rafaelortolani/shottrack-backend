package com.shottrack.backend.application.visit.dto;

import com.shottrack.backend.application.visit.model.TrainingStatus;

import java.time.Instant;
import java.util.UUID;

public record TrainingResponse(
        UUID id,
        UUID modalityId,
        String modalityName,
        TrainingStatus status,
        Instant startedAt,
        Instant endedAt
) {
}
