package com.shottrack.backend.application.dashboard.usecase;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ADR-0022: melhor valor de um tipo de resultado (ADR-0011) e quantos registros
 * válidos o sustentam, apurado por HighlightCalculator.bests.
 */
public record ResultTypeBest(UUID resultTypeId, String resultTypeName, BigDecimal value, int count) {
}
