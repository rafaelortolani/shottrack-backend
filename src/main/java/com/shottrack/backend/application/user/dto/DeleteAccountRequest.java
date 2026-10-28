package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "senha é obrigatória")
        String password
) {
}
