package com.shottrack.backend.application.dashboard.dto;

import java.math.BigDecimal;

public record ResultRecordResponse(
        String resultTypeName,
        BigDecimal value
) {
}
