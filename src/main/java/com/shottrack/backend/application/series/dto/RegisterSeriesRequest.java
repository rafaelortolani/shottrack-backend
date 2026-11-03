package com.shottrack.backend.application.series.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterSeriesRequest(
        @NotNull(message = "treino é obrigatório")
        UUID trainingId,

        UUID weaponId,
        UUID ammunitionId,
        @Digits(integer = 6, fraction = 2, message = "distância deve ter até 6 dígitos inteiros e 2 decimais")
        @DecimalMin(value = "0", message = "distância não pode ser negativa")
        @DecimalMax(value = "100000", message = "distância deve ser de no máximo 100000 metros")
        BigDecimal distanceMeters,
        @Size(max = 100, message = "alvo deve ter no máximo 100 caracteres")
        String target,
        Integer shotCount,
        @Size(max = 2000, message = "observações deve ter no máximo 2000 caracteres")
        String notes
) {
}
