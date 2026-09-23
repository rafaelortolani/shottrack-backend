package com.shottrack.backend.application.dashboard.dto;

import com.shottrack.backend.application.dashboard.model.MainActionType;

public record MainActionResponse(
        MainActionType type,
        ActiveVisitResponse activeVisit
) {
}
