package com.shottrack.backend.application.plan.dto;

import com.shottrack.backend.application.plan.model.PlanAuditAction;

import java.time.Instant;

/** UC67: uma entrada do histórico. Usuário excluído (UC52) vem como `deleted=true`, sem nome nem e-mail. */
public record PlanAuditResponse(
        Instant at,
        PlanAuditAction action,
        UserRef actor,
        String targetType,
        String targetPlanCode,
        UserRef targetUser,
        String field,
        String oldValue,
        String newValue
) {

    public record UserRef(String name, String email, boolean deleted) {
    }
}
