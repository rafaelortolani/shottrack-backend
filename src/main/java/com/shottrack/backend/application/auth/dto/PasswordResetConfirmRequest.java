package com.shottrack.backend.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "token é obrigatório")
        @Size(max = 255, message = "token deve ter no máximo 255 caracteres")
        String token,

        @NotBlank(message = "nova senha é obrigatória")
        @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
        @Size(max = 72, message = "{VALIDATION_PASSWORD_TOO_LONG}")
        String newPassword
) {
}
