package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "nova senha é obrigatória")
        @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
        @Size(max = 72, message = "{VALIDATION_PASSWORD_TOO_LONG}")
        String newPassword
) {
}
