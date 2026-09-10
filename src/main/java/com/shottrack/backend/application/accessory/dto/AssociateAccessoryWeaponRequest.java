package com.shottrack.backend.application.accessory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssociateAccessoryWeaponRequest(
        @NotNull(message = "arma é obrigatória")
        UUID weaponId
) {
}
