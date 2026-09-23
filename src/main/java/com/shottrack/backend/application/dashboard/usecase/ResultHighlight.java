package com.shottrack.backend.application.dashboard.usecase;

import java.math.BigDecimal;

/**
 * Melhor valor de um tipo de resultado (ADR-0011), apurado por
 * HighlightCalculator — usado tanto pro destaque quanto pros recordes.
 */
public record ResultHighlight(String resultTypeName, BigDecimal value) {
}
