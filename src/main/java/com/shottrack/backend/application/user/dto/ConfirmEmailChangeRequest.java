package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailChangeRequest(
        @NotBlank(message = "código é obrigatório")
        String code
) {
}
