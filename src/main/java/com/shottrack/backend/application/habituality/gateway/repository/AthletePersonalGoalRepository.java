package com.shottrack.backend.application.habituality.gateway.repository;

import com.shottrack.backend.application.habituality.model.AthletePersonalGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AthletePersonalGoalRepository extends JpaRepository<AthletePersonalGoal, UUID> {

    Optional<AthletePersonalGoal> findByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}
