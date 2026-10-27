package com.shottrack.backend.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "token é obrigatório")
        String token,

        @NotBlank(message = "nova senha é obrigatória")
        @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
        String newPassword
) {
}
