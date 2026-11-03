package com.shottrack.backend.application.user.dto;

import com.shottrack.backend.common.validation.EmailNormalizer;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        @Size(max = 254, message = "email deve ter no máximo 254 caracteres")
        String email,

        /** S02: troca de e-mail exige a senha atual (mesma regra do UC05). */
        @NotBlank(message = "senha atual é obrigatória")
        String currentPassword
) {
    public ChangeEmailRequest {
        email = EmailNormalizer.normalize(email);
    }
}
