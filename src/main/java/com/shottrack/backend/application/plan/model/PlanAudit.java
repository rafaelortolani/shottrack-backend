package com.shottrack.backend.application.plan.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * ADR-0027: registro de uma ação de administrador sobre planos. Ator e alvo-usuário são anulados pelo banco
 * (ON DELETE SET NULL) quando a conta é excluída (UC52); o registro permanece.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "plan_audit")
public class PlanAudit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, updatable = false)
    private final Instant at;

    @Column(name = "actor_user_id", updatable = false)
    private final UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private final PlanAuditAction action;

    @Column(name = "target_type", nullable = false, updatable = false)
    private final String targetType;

    @Column(name = "target_plan_code", updatable = false)
    private final String targetPlanCode;

    @Column(name = "target_user_id", updatable = false)
    private final UUID targetUserId;

    @Column(name = "field_key", updatable = false)
    private final String fieldKey;

    @Column(name = "old_value", updatable = false)
    private final String oldValue;

    @Column(name = "new_value", updatable = false)
    private final String newValue;

    @Builder
    private PlanAudit(UUID actorUserId, @NonNull PlanAuditAction action, @NonNull String targetType, String targetPlanCode,
                      UUID targetUserId, String fieldKey, String oldValue, String newValue) {
        this.at = Instant.now();
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetPlanCode = targetPlanCode;
        this.targetUserId = targetUserId;
        this.fieldKey = fieldKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
