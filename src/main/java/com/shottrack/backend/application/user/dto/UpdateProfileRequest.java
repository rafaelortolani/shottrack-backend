package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
        String name,

        @NotBlank(message = "nível de experiência é obrigatório")
        @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED", message = "nível de experiência inválido")
        String experienceLevel
) {
}
