package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailChangeRequest(
        @NotBlank(message = "código é obrigatório")
        @Size(max = 20, message = "código deve ter no máximo 20 caracteres")
        String code
) {
}
