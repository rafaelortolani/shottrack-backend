package com.shottrack.backend.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refresh token é obrigatório")
        String refreshToken
) {
}
