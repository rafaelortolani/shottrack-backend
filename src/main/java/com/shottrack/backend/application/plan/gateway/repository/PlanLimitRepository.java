package com.shottrack.backend.application.plan.gateway.repository;

import com.shottrack.backend.application.plan.model.PlanLimitId;
import com.shottrack.backend.application.plan.model.PlanLimitValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlanLimitRepository extends JpaRepository<PlanLimitValue, PlanLimitId> {

    List<PlanLimitValue> findAllByIdPlanId(UUID planId);

    List<PlanLimitValue> findAllByIdPlanIdIn(Collection<UUID> planIds);
}
