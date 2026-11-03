package com.shottrack.backend.application.user.dto;

import com.shottrack.backend.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "nova senha é obrigatória")
        @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
        @MaxUtf8Bytes(72)
        String newPassword
) {
}
