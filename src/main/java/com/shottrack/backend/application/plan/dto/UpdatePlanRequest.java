package com.shottrack.backend.application.plan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** UC69: edição parcial; campo ausente (null) = não mexe. */
public record UpdatePlanRequest(
        String name,
        Boolean active,
        @JsonProperty("default") Boolean defaultPlan
) {
}
