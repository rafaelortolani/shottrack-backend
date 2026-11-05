package com.shottrack.backend.application.plan.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** UC64: os valores chegam como JSON cru para recusar texto/decimal com PLAN_LIMIT_OUT_OF_RANGE (e não com erro de formato). */
public record EditPlanLimitsRequest(
        @NotEmpty(message = "informe ao menos um limite")
        Map<String, JsonNode> limits
) {
}
