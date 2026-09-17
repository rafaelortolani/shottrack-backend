package com.shottrack.backend.application.accessory.dto;

import java.util.UUID;

/**
 * Edição parcial (UC20): campo ausente/null significa "não alterar". Nome,
 * se enviado, não pode ficar em branco — validado no usecase, não aqui, já
 * que @NotBlank rejeitaria também o caso legítimo de não enviar o campo.
 * TypeId, se enviado, precisa existir no catálogo (UC28) — também validado
 * no usecase.
 */
public record AccessoryUpdateRequest(
        String name,
        UUID typeId,
        String notes
) {
}
