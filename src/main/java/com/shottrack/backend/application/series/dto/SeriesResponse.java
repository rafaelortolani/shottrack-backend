package com.shottrack.backend.application.series.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SeriesResponse(
        UUID id,
        UUID trainingId,
        UUID weaponId,
        UUID ammunitionId,
        BigDecimal distanceMeters,
        String target,
        Integer shotCount,
        String notes,
        Instant createdAt,
        List<SeriesResultResponse> results
) {
}
