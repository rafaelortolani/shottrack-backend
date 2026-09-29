package com.shottrack.backend.application.dashboard.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC48: parâmetros de query de GET /api/dashboard/weapon-comparison.
 * Período opcional, inclusivo nas duas pontas — mesmo formato do filtro
 * de período do histórico de séries (UC47).
 */
public record WeaponComparisonRequest(
        @NotNull(message = "modalidade é obrigatória")
        UUID modalityId,

        @NotNull(message = "tipo de resultado é obrigatório")
        UUID resultTypeId,

        @NotNull(message = "informe pelo menos 2 armas para comparar")
        List<UUID> weaponIds,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
) {

    /**
     * Conta armas distintas — repetir a mesma arma não vira comparação.
     */
    @AssertTrue(message = "informe pelo menos 2 armas para comparar")
    public boolean isEnoughWeapons() {
        return weaponIds == null || weaponIds.stream().distinct().count() >= 2;
    }

    @AssertTrue(message = "data de início não pode ser depois da data de fim")
    public boolean isPeriodValid() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
