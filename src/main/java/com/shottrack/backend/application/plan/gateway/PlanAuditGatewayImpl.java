package com.shottrack.backend.application.plan.gateway;

import com.shottrack.backend.application.plan.gateway.repository.PlanAuditRepository;
import com.shottrack.backend.application.plan.model.PlanAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class PlanAuditGatewayImpl implements PlanAuditGateway {

    private final PlanAuditRepository planAuditRepository;

    @Override
    public PlanAudit save(PlanAudit audit) {
        return planAuditRepository.save(audit);
    }

    @Override
    public List<PlanAudit> findRecent(int limit) {
        return planAuditRepository.findAllByOrderByAtDescIdDesc(PageRequest.of(0, limit));
    }
}
