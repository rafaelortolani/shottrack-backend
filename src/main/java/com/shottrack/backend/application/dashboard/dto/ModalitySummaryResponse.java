package com.shottrack.backend.application.dashboard.dto;

public record ModalitySummaryResponse(
        String modalityName,
        int trainingCount,
        DashboardHighlightResponse best
) {
}
