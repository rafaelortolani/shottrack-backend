package com.shottrack.backend.application.series.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterSeriesResultRequest(
        @NotNull(message = "tipo de resultado é obrigatório")
        UUID resultTypeId,

        @NotBlank(message = "valor é obrigatório")
        String value
) {
}
