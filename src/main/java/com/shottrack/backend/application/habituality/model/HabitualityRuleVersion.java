package com.shottrack.backend.application.habituality.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ADR-0023: versão da regra, vigente a partir de validFrom. A versão em vigor é a de
 * maior validFrom que não passa de hoje.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "habituality_rule_versions")
public class HabitualityRuleVersion extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private final UUID ruleId;

    @Setter
    @NonNull
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Setter
    @Column(name = "window_months", nullable = false)
    private int windowMonths;

    @Builder
    private HabitualityRuleVersion(UUID ruleId, @NonNull LocalDate validFrom, int windowMonths) {
        this.ruleId = ruleId;
        this.validFrom = validFrom;
        this.windowMonths = windowMonths;
    }
}
