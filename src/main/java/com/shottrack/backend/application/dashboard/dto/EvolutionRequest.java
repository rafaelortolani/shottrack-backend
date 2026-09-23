package com.shottrack.backend.application.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * UC46: parâmetros de query de GET /api/dashboard/evolution.
 */
public record EvolutionRequest(
        @NotNull(message = "modalidade é obrigatória")
        UUID modalityId,

        @NotNull(message = "tipo de resultado é obrigatório")
        UUID resultTypeId,

        @NotBlank(message = "período é obrigatório")
        @Pattern(regexp = "7d|30d|3m|1a", message = "período inválido")
        String period,

        @NotBlank(message = "modo é obrigatório")
        @Pattern(regexp = "MEDIA|MELHOR", message = "modo inválido")
        String mode
) {
}
