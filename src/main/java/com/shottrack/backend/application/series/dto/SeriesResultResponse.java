package com.shottrack.backend.application.series.dto;

import java.util.UUID;

public record SeriesResultResponse(
        UUID resultTypeId,
        String resultTypeName,
        String value,
        boolean notApplicable
) {
}
