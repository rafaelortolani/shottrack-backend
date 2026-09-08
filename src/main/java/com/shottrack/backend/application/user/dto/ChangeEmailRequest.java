package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email
) {
}
