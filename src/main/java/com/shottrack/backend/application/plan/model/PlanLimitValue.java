package com.shottrack.backend.application.plan.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ADR-0027: valor de um limite num plano (só existe linha para o que foi gravado; o resto usa o padrão do enum). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "plan_limits")
public class PlanLimitValue {

    @EmbeddedId
    private final PlanLimitId id;

    @Setter
    @Column(nullable = false)
    private long value;

    @Builder
    private PlanLimitValue(PlanLimitId id, long value) {
        this.id = id;
        this.value = value;
    }
}
