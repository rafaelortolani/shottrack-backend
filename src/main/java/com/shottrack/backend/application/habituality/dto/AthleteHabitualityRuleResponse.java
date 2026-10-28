package com.shottrack.backend.application.habituality.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC56: regra ativa com a versão em vigor (níveis e exigências) e, se houver, a próxima.
 */
public record AthleteHabitualityRuleResponse(
        UUID id,
        String name,
        String legalReference,
        LocalDate validFrom,
        int windowMonths,
        List<HabitualityLevelResponse> levels,
        UpcomingVersionResponse upcomingVersion
) {
}
