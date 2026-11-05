package com.shottrack.backend.application.plan.gateway;

import com.shottrack.backend.application.plan.gateway.repository.PlanLimitRepository;
import com.shottrack.backend.application.plan.model.PlanLimitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PlanLimitGatewayImpl implements PlanLimitGateway {

    private final PlanLimitRepository planLimitRepository;

    @Override
    public List<PlanLimitValue> findAllByPlanId(UUID planId) {
        return planLimitRepository.findAllByIdPlanId(planId);
    }

    @Override
    public List<PlanLimitValue> findAllByPlanIds(Collection<UUID> planIds) {
        return planIds.isEmpty() ? List.of() : planLimitRepository.findAllByIdPlanIdIn(planIds);
    }

    @Override
    public PlanLimitValue save(PlanLimitValue value) {
        return planLimitRepository.save(value);
    }
}
