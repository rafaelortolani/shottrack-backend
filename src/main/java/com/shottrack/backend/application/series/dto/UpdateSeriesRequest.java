package com.shottrack.backend.application.series.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Edição parcial (UC38/ADR-0013): campo ausente/null significa "não
 * alterar" — é assim que "completar depois" funciona na prática.
 */
public record UpdateSeriesRequest(
        UUID weaponId,
        UUID ammunitionId,
        BigDecimal distanceMeters,
        String target,
        Integer shotCount,
        String notes
) {
}
