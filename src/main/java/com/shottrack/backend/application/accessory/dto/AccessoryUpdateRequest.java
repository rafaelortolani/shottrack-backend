package com.shottrack.backend.application.accessory.dto;

/**
 * Edição parcial (UC20): campo ausente/null significa "não alterar". Nome,
 * se enviado, não pode ficar em branco — validado no usecase, não aqui, já
 * que @NotBlank rejeitaria também o caso legítimo de não enviar o campo.
 */
public record AccessoryUpdateRequest(
        String name,
        String type,
        String notes
) {
}
