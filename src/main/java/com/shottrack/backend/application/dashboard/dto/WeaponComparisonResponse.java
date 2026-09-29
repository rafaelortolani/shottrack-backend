package com.shottrack.backend.application.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;

import java.math.BigDecimal;

/**
 * UC48: uma entrada por arma, na ordem em que foram informadas.
 */
public record WeaponComparisonResponse(
        WeaponResponse weapon,
        int seriesCount,
        // UC48 (3a): ausente quando a arma não tem valor numérico registrado
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal average,
        // idem, e também pra tipo sem orientação (ADR-0011)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal best
) {
}
