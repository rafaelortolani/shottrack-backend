package com.shottrack.backend.application.plan.usecase;

import com.shottrack.backend.application.plan.gateway.PlanAuditGateway;
import com.shottrack.backend.application.plan.model.PlanAudit;
import com.shottrack.backend.application.plan.model.PlanAuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** ADR-0027: grava as entradas de auditoria (sempre na transação da própria alteração). Nunca registra "sem mudança". */
@Component
@RequiredArgsConstructor
public class PlanAuditRecorder {

    private final PlanAuditGateway auditGateway;

    public void limitChanged(UUID actorId, String planCode, String limitKey, long oldValue, long newValue) {
        auditGateway.save(PlanAudit.builder().actorUserId(actorId).action(PlanAuditAction.LIMIT_CHANGED).targetType("PLAN")
                .targetPlanCode(planCode).fieldKey(limitKey).oldValue(String.valueOf(oldValue)).newValue(String.valueOf(newValue)).build());
    }

    public void planAssigned(UUID actorId, UUID athleteId, String oldPlanCode, String newPlanCode) {
        auditGateway.save(PlanAudit.builder().actorUserId(actorId).action(PlanAuditAction.PLAN_ASSIGNED).targetType("USER")
                .targetUserId(athleteId).oldValue(oldPlanCode).newValue(newPlanCode).build());
    }

    public void planCreated(UUID actorId, String planCode, String name) {
        auditGateway.save(PlanAudit.builder().actorUserId(actorId).action(PlanAuditAction.PLAN_CREATED).targetType("PLAN")
                .targetPlanCode(planCode).newValue(name).build());
    }

    public void planUpdated(UUID actorId, String planCode, String field, Object oldValue, Object newValue) {
        auditGateway.save(PlanAudit.builder().actorUserId(actorId).action(PlanAuditAction.PLAN_UPDATED).targetType("PLAN")
                .targetPlanCode(planCode).fieldKey(field).oldValue(String.valueOf(oldValue)).newValue(String.valueOf(newValue)).build());
    }
}
