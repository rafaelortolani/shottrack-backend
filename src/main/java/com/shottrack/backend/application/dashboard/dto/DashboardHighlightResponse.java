package com.shottrack.backend.application.dashboard.dto;

import java.math.BigDecimal;

public record DashboardHighlightResponse(
        String resultTypeName,
        BigDecimal value
) {
}
