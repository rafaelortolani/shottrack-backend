package com.shottrack.backend.application.auth.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refresh token é obrigatório")
        @Size(max = 255, message = "refresh token deve ter no máximo 255 caracteres")
        String refreshToken
) {
}
