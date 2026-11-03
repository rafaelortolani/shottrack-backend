package com.shottrack.backend.application.traininglocation.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record TrainingLocationRegisterRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String name,

        @NotBlank(message = "cidade é obrigatória")
        @Size(max = 100, message = "cidade deve ter no máximo 100 caracteres")
        String city,

        @NotBlank(message = "estado é obrigatório")
        @Size(max = 50, message = "estado deve ter no máximo 50 caracteres")
        String state,

        /** ADR-0023: ausente = false. */
        Boolean club
) {

    public TrainingLocationRegisterRequest(String name, String city, String state) {
        this(name, city, state, null);
    }
}
