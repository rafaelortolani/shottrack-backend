package com.shottrack.backend.application.habituality.gateway;

import com.shottrack.backend.application.habituality.gateway.repository.AthleteHabitualityTrackingRepository;
import com.shottrack.backend.application.habituality.gateway.repository.AthletePersonalGoalRepository;
import com.shottrack.backend.application.habituality.model.AthleteHabitualityTracking;
import com.shottrack.backend.application.habituality.model.AthletePersonalGoal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AthleteHabitualityGatewayImpl implements AthleteHabitualityGateway {

    private final AthleteHabitualityTrackingRepository trackingRepository;
    private final AthletePersonalGoalRepository goalRepository;

    @Override
    public Optional<AthleteHabitualityTracking> findTrackingByUserId(UUID userId) {
        return trackingRepository.findByUserId(userId);
    }

    @Override
    public AthleteHabitualityTracking saveTracking(AthleteHabitualityTracking tracking) {
        return trackingRepository.save(tracking);
    }

    @Override
    public void deleteTrackingByUserId(UUID userId) {
        trackingRepository.deleteAllByUserId(userId);
    }

    @Override
    public boolean existsTrackingByRuleId(UUID ruleId) {
        return trackingRepository.existsByRuleId(ruleId);
    }

    @Override
    public Optional<AthletePersonalGoal> findGoalByUserId(UUID userId) {
        return goalRepository.findByUserId(userId);
    }

    @Override
    public AthletePersonalGoal saveGoal(AthletePersonalGoal goal) {
        return goalRepository.save(goal);
    }

    @Override
    public void deleteGoalByUserId(UUID userId) {
        goalRepository.deleteAllByUserId(userId);
    }
}
