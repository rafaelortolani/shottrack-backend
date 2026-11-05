package com.shottrack.backend.application.plan.gateway;

import com.shottrack.backend.application.plan.model.PlanAudit;

import java.util.List;

public interface PlanAuditGateway {

    PlanAudit save(PlanAudit audit);

    /** Mais recentes primeiro. */
    List<PlanAudit> findRecent(int limit);
}
