package com.shottrack.backend.application.traininglocation.dto;

import jakarta.validation.constraints.NotBlank;

public record TrainingLocationRegisterRequest(
        @NotBlank(message = "nome é obrigatório")
        String name,

        @NotBlank(message = "cidade é obrigatória")
        String city,

        @NotBlank(message = "estado é obrigatório")
        String state,

        /** ADR-0023: ausente = false. */
        Boolean club
) {

    public TrainingLocationRegisterRequest(String name, String city, String state) {
        this(name, city, state, null);
    }
}
