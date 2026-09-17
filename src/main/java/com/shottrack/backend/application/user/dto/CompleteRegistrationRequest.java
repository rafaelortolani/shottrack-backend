package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRegistrationRequest(
        @NotBlank(message = "token é obrigatório")
        String token,

        @NotBlank(message = "nome é obrigatório")
        String name,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
        String password
) {
}
