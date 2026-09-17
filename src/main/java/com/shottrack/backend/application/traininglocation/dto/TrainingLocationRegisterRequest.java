package com.shottrack.backend.application.traininglocation.dto;

import jakarta.validation.constraints.NotBlank;

public record TrainingLocationRegisterRequest(
        @NotBlank(message = "nome é obrigatório")
        String name,

        @NotBlank(message = "cidade é obrigatória")
        String city,

        @NotBlank(message = "estado é obrigatório")
        String state
) {
}
