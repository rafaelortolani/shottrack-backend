package com.shottrack.backend.application.habituality.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HabitualityTrackingRequest(
        @NotNull(message = "regra é obrigatória")
        UUID ruleId,

        @NotBlank(message = "nível é obrigatório")
        @Size(max = 100, message = "nível deve ter no máximo 100 caracteres")
        String levelName
) {
}
