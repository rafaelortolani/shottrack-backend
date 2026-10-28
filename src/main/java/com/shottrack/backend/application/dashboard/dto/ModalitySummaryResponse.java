package com.shottrack.backend.application.dashboard.dto;

import java.util.UUID;

public record ModalitySummaryResponse(
        UUID modalityId,
        String modalityName,
        int trainingCount,
        DashboardHighlightResponse best
) {
}
