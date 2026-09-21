package com.shottrack.backend.application.visit.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenTrainingRequest(
        @NotNull(message = "visita é obrigatória")
        UUID visitId,

        @NotNull(message = "modalidade é obrigatória")
        UUID modalityId
) {
}
