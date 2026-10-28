package com.shottrack.backend.application.habituality.dto;

import com.shottrack.backend.application.habituality.model.ActivityType;
import com.shottrack.backend.application.visit.model.CompetitionScope;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * UC55: onlyInClub/distinctEvents ausentes = false. minScope só com COMPETICAO/QUALQUER
 * (validado no usecase).
 */
public record HabitualityRequirementRequest(
        @NotNull(message = "tipo de atividade é obrigatório")
        ActivityType activityType,

        @NotNull(message = "quantidade mínima é obrigatória")
        @Min(value = 1, message = "quantidade mínima deve ser pelo menos 1")
        Integer minQuantity,

        Boolean onlyInClub,

        Boolean distinctEvents,

        CompetitionScope minScope
) {
}
