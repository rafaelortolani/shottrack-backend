package com.shottrack.backend.application.plan.dto;

/** UC68: código e nome são validados no usecase (erros próprios PLAN_CODE_INVALID / PLAN_NAME_INVALID). */
public record CreatePlanRequest(
        String code,
        String name,
        String copyFromCode
) {
}
