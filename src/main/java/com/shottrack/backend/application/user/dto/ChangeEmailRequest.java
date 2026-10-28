package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,

        /** S02: troca de e-mail exige a senha atual (mesma regra do UC05). */
        @NotBlank(message = "senha atual é obrigatória")
        String currentPassword
) {
}
