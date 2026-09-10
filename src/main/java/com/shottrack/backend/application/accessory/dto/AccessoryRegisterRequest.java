package com.shottrack.backend.application.accessory.dto;

import jakarta.validation.constraints.NotBlank;

public record AccessoryRegisterRequest(
        @NotBlank(message = "nome é obrigatório")
        String name,

        String type,

        String notes
) {
}
