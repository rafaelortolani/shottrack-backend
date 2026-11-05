package com.shottrack.backend.application.plan.gateway;

import com.shottrack.backend.application.plan.model.PlanLimitValue;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlanLimitGateway {

    List<PlanLimitValue> findAllByPlanId(UUID planId);

    List<PlanLimitValue> findAllByPlanIds(Collection<UUID> planIds);

    PlanLimitValue save(PlanLimitValue value);
}
