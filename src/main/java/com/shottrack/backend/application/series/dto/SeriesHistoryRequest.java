package com.shottrack.backend.application.series.dto;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * UC47: parâmetros de query de GET /api/series — todos opcionais e
 * combináveis livremente. Período e distância são inclusivos nas duas
 * pontas.
 */
public record SeriesHistoryRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        UUID modalityId,

        UUID weaponId,

        BigDecimal minDistanceMeters,

        BigDecimal maxDistanceMeters
) {

    @AssertTrue(message = "data de início não pode ser depois da data de fim")
    public boolean isPeriodValid() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
