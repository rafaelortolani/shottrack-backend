package com.shottrack.backend.application.plan.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignPlanRequest(
        @NotBlank(message = "plano é obrigatório")
        String planCode
) {
}
