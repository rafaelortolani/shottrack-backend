package com.shottrack.backend.application.weapon.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * UC10/ADR-0004 (Revisão 2): tipo e marca são derivados do modelo no
 * servidor.
 */
public record WeaponUpdateRequest(
        @NotNull(message = "modelo é obrigatório")
        UUID modelId,
        @NotNull(message = "calibre é obrigatório")
        UUID caliberId,
        String nickname
) {
}
