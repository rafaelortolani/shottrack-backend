package com.shottrack.backend.application.habituality.gateway.repository;

import com.shottrack.backend.application.habituality.model.HabitualityLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HabitualityLevelRepository extends JpaRepository<HabitualityLevel, UUID> {

    List<HabitualityLevel> findAllByVersionIdIn(Collection<UUID> versionIds);

    void deleteAllByVersionId(UUID versionId);
}
