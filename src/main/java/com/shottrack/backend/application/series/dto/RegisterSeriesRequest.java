package com.shottrack.backend.application.series.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterSeriesRequest(
        @NotNull(message = "treino é obrigatório")
        UUID trainingId,

        UUID weaponId,
        UUID ammunitionId,
        BigDecimal distanceMeters,
        String target,
        Integer shotCount,
        String notes
) {
}
