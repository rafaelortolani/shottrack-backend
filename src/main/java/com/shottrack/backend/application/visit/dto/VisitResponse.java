package com.shottrack.backend.application.visit.dto;

import com.shottrack.backend.application.visit.model.VisitStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID trainingLocationId,
        String observations,
        VisitStatus status,
        Instant startedAt,
        Instant endedAt,
        List<TrainingSummaryResponse> trainings
) {
}
