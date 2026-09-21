package com.shottrack.backend.application.visit.dto;

import java.time.Instant;
import java.util.UUID;

public record TrainingSummaryResponse(
        UUID id,
        String modalityName,
        String status,
        Instant startedAt,
        Instant endedAt
) {
}
