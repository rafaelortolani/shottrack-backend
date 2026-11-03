package com.shottrack.backend.application.series.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterSeriesResultRequest(
        @NotNull(message = "tipo de resultado é obrigatório")
        UUID resultTypeId,

        @NotBlank(message = "valor é obrigatório")
        @Size(max = 500, message = "valor deve ter no máximo 500 caracteres")
        String value
) {
}
