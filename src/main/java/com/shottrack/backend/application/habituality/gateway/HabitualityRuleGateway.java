package com.shottrack.backend.application.habituality.gateway;

import com.shottrack.backend.application.habituality.model.HabitualityRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitualityRuleGateway {

    HabitualityRule save(HabitualityRule rule);

    Optional<HabitualityRule> findById(UUID id);

    List<HabitualityRule> findAll();

    List<HabitualityRule> findAllActive();

    boolean existsByName(String name);

    boolean existsByNameExcluding(String name, UUID excludedId);
}
