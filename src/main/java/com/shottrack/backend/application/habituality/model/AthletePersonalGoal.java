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
 * ADR-0023 (UC58): meta pessoal de treinos por ano civil — dado do atleta, independe
 * de regra e nível.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "athlete_personal_goal")
public class AthletePersonalGoal extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Setter
    @Column(name = "yearly_trainings", nullable = false)
    private int yearlyTrainings;

    @Builder
    private AthletePersonalGoal(UUID userId, int yearlyTrainings) {
        this.userId = userId;
        this.yearlyTrainings = yearlyTrainings;
    }
}
