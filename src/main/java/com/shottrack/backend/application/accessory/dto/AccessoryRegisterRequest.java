package com.shottrack.backend.application.accessory.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccessoryRegisterRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
        String name,

        @NotNull(message = "tipo é obrigatório")
        UUID typeId,

        @Size(max = 2000, message = "observações deve ter no máximo 2000 caracteres")
        String notes
) {
}
