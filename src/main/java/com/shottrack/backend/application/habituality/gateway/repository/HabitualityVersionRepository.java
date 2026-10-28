package com.shottrack.backend.application.habituality.gateway.repository;

import com.shottrack.backend.application.habituality.model.HabitualityRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HabitualityVersionRepository extends JpaRepository<HabitualityRuleVersion, UUID> {

    List<HabitualityRuleVersion> findAllByRuleIdOrderByValidFromAsc(UUID ruleId);

    List<HabitualityRuleVersion> findAllByRuleIdIn(Collection<UUID> ruleIds);

    boolean existsByRuleIdAndValidFrom(UUID ruleId, LocalDate validFrom);

    boolean existsByRuleIdAndValidFromAndIdNot(UUID ruleId, LocalDate validFrom, UUID id);

    boolean existsByRuleId(UUID ruleId);
}
