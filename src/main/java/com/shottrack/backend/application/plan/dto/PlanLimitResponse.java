package com.shottrack.backend.application.plan.dto;

/** UC63: um limite do plano com o valor efetivo e a faixa; `customized` = o valor difere do padrão do enum. */
public record PlanLimitResponse(
        String key,
        long value,
        String unit,
        long min,
        long max,
        long defaultValue,
        boolean enforced,
        boolean customized
) {
}
