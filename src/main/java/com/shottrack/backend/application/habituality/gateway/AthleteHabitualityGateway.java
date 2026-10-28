package com.shottrack.backend.application.habituality.gateway;

import com.shottrack.backend.application.habituality.model.AthleteHabitualityTracking;
import com.shottrack.backend.application.habituality.model.AthletePersonalGoal;

import java.util.Optional;
import java.util.UUID;

/**
 * Dados do atleta na habitualidade (UC56/UC58): acompanhamento e meta pessoal.
 */
public interface AthleteHabitualityGateway {

    Optional<AthleteHabitualityTracking> findTrackingByUserId(UUID userId);

    AthleteHabitualityTracking saveTracking(AthleteHabitualityTracking tracking);

    void deleteTrackingByUserId(UUID userId);

    boolean existsTrackingByRuleId(UUID ruleId);

    Optional<AthletePersonalGoal> findGoalByUserId(UUID userId);

    AthletePersonalGoal saveGoal(AthletePersonalGoal goal);

    void deleteGoalByUserId(UUID userId);
}
