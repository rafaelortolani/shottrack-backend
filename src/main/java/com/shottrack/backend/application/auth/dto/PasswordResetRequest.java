package com.shottrack.backend.application.auth.dto;

import com.shottrack.backend.common.validation.EmailNormalizer;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        @Size(max = 254, message = "email deve ter no máximo 254 caracteres")
        String email
) {
    public PasswordResetRequest {
        email = EmailNormalizer.normalize(email);
    }
}
