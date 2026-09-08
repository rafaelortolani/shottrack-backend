package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateNameRequest(
        @NotBlank(message = "nome é obrigatório")
        String name
) {
}
