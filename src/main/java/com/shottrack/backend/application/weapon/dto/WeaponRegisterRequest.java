package com.shottrack.backend.application.weapon.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * UC06/ADR-0004 (Revisão 2): tipo e marca são derivados do modelo no
 * servidor. Apelido não entra no cadastro — só existe a partir da edição
 * (UC10).
 */
public record WeaponRegisterRequest(
        @NotNull(message = "modelo é obrigatório")
        UUID modelId,
        @NotNull(message = "calibre é obrigatório")
        UUID caliberId
) {
}
