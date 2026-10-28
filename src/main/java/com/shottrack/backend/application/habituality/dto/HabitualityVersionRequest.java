package com.shottrack.backend.application.habituality.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record HabitualityVersionRequest(
        @NotNull(message = "data de vigência é obrigatória")
        LocalDate validFrom,

        @NotNull(message = "janela em meses é obrigatória")
        @Min(value = 1, message = "janela deve ser de pelo menos 1 mês")
        Integer windowMonths,

        @NotEmpty(message = "a versão precisa de pelo menos um nível")
        List<@NotNull(message = "nível inválido") @Valid HabitualityLevelRequest> levels
) {
}
