package com.shottrack.backend.application.dashboard.dto;

/**
 * UC42 v7: valor do período atual e do período anterior de mesma duração.
 */
public record IndicatorResponse(
        long value,
        long previous
) {
}
