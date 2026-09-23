package com.shottrack.backend.application.dashboard.usecase;

import java.math.BigDecimal;

/**
 * Destaque (ADR-0011) calculado por HighlightCalculator — o tipo de
 * resultado escolhido e o melhor valor registrado nele.
 */
public record ResultHighlight(String resultTypeName, BigDecimal value) {
}
