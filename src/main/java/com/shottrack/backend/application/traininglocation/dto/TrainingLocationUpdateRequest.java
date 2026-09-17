package com.shottrack.backend.application.traininglocation.dto;

/**
 * Edição parcial (UC26): campo ausente/null significa "não alterar". Todos
 * os campos, se enviados, não podem ficar em branco — validado no usecase,
 * não aqui, já que @NotBlank rejeitaria também o caso legítimo de não
 * enviar o campo.
 */
public record TrainingLocationUpdateRequest(
        String name,
        String city,
        String state
) {
}
