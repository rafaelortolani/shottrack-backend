package com.shottrack.backend.application.habituality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UC54: active ausente = true.
 */
public record HabitualityRuleRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String name,

        @Size(max = 500, message = "referência legal deve ter no máximo 500 caracteres")
        String legalReference,

        Boolean active
) {
}
