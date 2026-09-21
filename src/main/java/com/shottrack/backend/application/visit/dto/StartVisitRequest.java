package com.shottrack.backend.application.visit.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartVisitRequest(
        @NotNull(message = "local de treino é obrigatório")
        UUID trainingLocationId,

        String observations
) {
}
