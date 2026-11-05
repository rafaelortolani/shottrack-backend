package com.shottrack.backend.application.plan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** UC63/UC64/UC68/UC69: plano com seus limites efetivos. Sem ids internos. */
public record PlanResponse(
        String code,
        String name,
        boolean active,
        @JsonProperty("default") boolean defaultPlan,
        long athleteCount,
        List<PlanLimitResponse> limits
) {
}
