package com.shottrack.backend.application.traininglocation.dto;

import jakarta.validation.constraints.Size;

/**
 * Edição parcial (UC26): campo ausente/null significa "não alterar". Todos
 * os campos, se enviados, não podem ficar em branco — validado no usecase,
 * não aqui, já que @NotBlank rejeitaria também o caso legítimo de não
 * enviar o campo.
 */
public record TrainingLocationUpdateRequest(
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String name,
        @Size(max = 100, message = "cidade deve ter no máximo 100 caracteres")
        String city,
        @Size(max = 50, message = "estado deve ter no máximo 50 caracteres")
        String state,
        Boolean club
) {

    public TrainingLocationUpdateRequest(String name, String city, String state) {
        this(name, city, state, null);
    }
}
