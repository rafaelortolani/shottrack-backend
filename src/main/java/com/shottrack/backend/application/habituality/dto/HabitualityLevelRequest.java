package com.shottrack.backend.application.habituality.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HabitualityLevelRequest(
        @NotBlank(message = "nome do nível é obrigatório")
        @Size(max = 100, message = "nome do nível deve ter no máximo 100 caracteres")
        String name,

        @NotNull(message = "posição do nível é obrigatória")
        Integer position,

        @NotEmpty(message = "cada nível precisa de pelo menos uma exigência")
        List<@NotNull(message = "exigência inválida") @Valid HabitualityRequirementRequest> requirements
) {
}
