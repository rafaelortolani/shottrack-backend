package com.shottrack.backend.application.visit.dto;

import com.shottrack.backend.application.visit.model.CompetitionScope;
import com.shottrack.backend.application.visit.model.VisitStatus;
import com.shottrack.backend.application.visit.model.VisitType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID trainingLocationId,
        String observations,
        VisitStatus status,
        VisitType visitType,
        CompetitionScope competitionScope,
        Instant startedAt,
        Instant endedAt,
        List<TrainingResponse> trainings
) {
}
