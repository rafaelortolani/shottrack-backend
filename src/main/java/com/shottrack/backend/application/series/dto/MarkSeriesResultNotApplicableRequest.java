package com.shottrack.backend.application.series.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkSeriesResultNotApplicableRequest(
        @NotNull(message = "tipo de resultado é obrigatório")
        UUID resultTypeId
) {
}
