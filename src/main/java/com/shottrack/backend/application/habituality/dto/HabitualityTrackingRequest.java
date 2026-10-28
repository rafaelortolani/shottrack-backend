package com.shottrack.backend.application.habituality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HabitualityTrackingRequest(
        @NotNull(message = "regra é obrigatória")
        UUID ruleId,

        @NotBlank(message = "nível é obrigatório")
        String levelName
) {
}
