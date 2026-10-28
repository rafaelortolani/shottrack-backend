package com.shottrack.backend.application.habituality.gateway.repository;

import com.shottrack.backend.application.habituality.model.HabitualityRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HabitualityRequirementRepository extends JpaRepository<HabitualityRequirement, UUID> {

    List<HabitualityRequirement> findAllByLevelIdIn(Collection<UUID> levelIds);

    void deleteAllByLevelIdIn(Collection<UUID> levelIds);
}
