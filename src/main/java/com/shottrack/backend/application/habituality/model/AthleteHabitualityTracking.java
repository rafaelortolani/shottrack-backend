package com.shottrack.backend.application.habituality.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.UUID;

/**
 * ADR-0023: regra e nível que o atleta acompanha (um por atleta). O nível é guardado
 * pelo nome, que sobrevive à troca de versão.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "athlete_habituality_tracking")
public class AthleteHabitualityTracking extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Setter
    @NonNull
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Setter
    @NonNull
    @Column(name = "level_name", nullable = false)
    private String levelName;

    @Builder
    private AthleteHabitualityTracking(UUID userId, @NonNull UUID ruleId, @NonNull String levelName) {
        this.userId = userId;
        this.ruleId = ruleId;
        this.levelName = levelName;
    }
}
