package com.shottrack.backend.application.plan.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record PlanLimitId(
        @Column(name = "plan_id") UUID planId,
        @Column(name = "limit_key") String limitKey
) implements Serializable {
}
