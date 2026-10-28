package com.shottrack.backend.application.habituality.gateway.repository;

import com.shottrack.backend.application.habituality.model.AthleteHabitualityTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AthleteHabitualityTrackingRepository extends JpaRepository<AthleteHabitualityTracking, UUID> {

    Optional<AthleteHabitualityTracking> findByUserId(UUID userId);

    boolean existsByRuleId(UUID ruleId);

    void deleteAllByUserId(UUID userId);
}
