package com.shottrack.backend.application.weapon.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WeaponRegisterRequest(
        @NotNull(message = "tipo é obrigatório")
        UUID typeId,

        @NotNull(message = "marca é obrigatória")
        UUID brandId,

        @NotNull(message = "modelo é obrigatório")
        UUID modelId,

        @NotNull(message = "calibre é obrigatório")
        UUID caliberId
) {
}
