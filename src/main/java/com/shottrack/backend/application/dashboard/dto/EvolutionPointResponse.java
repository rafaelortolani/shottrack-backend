package com.shottrack.backend.application.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvolutionPointResponse(
        LocalDate date,
        BigDecimal value
) {
}
