package com.shottrack.backend.application.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

public record RecentTrainingResponse(
        UUID trainingId,
        Instant startedAt,
        String trainingLocationName,
        String modalityName,
        // UC42: ausente quando o treino não tem resultado elegível
        @JsonInclude(JsonInclude.Include.NON_NULL)
        DashboardHighlightResponse highlight
) {
}
