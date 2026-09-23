package com.shottrack.backend.application.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActiveVisitResponse(
        UUID visitId,
        String trainingLocationName,
        Instant startedAt,
        List<String> activeTrainingModalityNames
) {
}
