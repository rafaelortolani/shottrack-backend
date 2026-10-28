package com.shottrack.backend.application.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * UC53/S12: a exportação leva todos os dados do atleta — exige a senha atual, como a exclusão (UC52).
 */
public record ExportDataRequest(
        @NotBlank(message = "senha atual é obrigatória")
        String currentPassword
) {
}
