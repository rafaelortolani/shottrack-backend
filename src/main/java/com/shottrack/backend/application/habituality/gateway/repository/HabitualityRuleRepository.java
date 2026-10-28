package com.shottrack.backend.application.habituality.gateway.repository;

import com.shottrack.backend.application.habituality.model.HabitualityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HabitualityRuleRepository extends JpaRepository<HabitualityRule, UUID> {

    List<HabitualityRule> findAllByOrderByNameAsc();

    List<HabitualityRule> findAllByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
