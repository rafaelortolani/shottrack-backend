package com.shottrack.backend.application.accessory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccessoryRegisterRequest(
        @NotBlank(message = "nome é obrigatório")
        String name,

        @NotNull(message = "tipo é obrigatório")
        UUID typeId,

        String notes
) {
}
