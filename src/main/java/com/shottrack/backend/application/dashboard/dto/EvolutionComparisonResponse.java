package com.shottrack.backend.application.dashboard.dto;

import java.util.List;

/**
 * UC46: resposta de GET /api/dashboard/evolution com comparePrevious=true —
 * o período pedido e o imediatamente anterior, de mesma duração.
 */
public record EvolutionComparisonResponse(
        List<EvolutionPointResponse> points,
        List<EvolutionPointResponse> previousPoints
) {
}
