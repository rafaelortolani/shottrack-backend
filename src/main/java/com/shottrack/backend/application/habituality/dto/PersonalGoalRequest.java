package com.shottrack.backend.application.habituality.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PersonalGoalRequest(
        @NotNull(message = "meta anual é obrigatória")
        @Min(value = 1, message = "meta anual deve ser de pelo menos 1 treino")
        @Max(value = 1000, message = "meta anual deve ser de no máximo 1000 treinos")
        Integer yearlyTrainings
) {
}
