package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.model.ModalityStats;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado do recálculo (ADR-0015) — mesma forma usada pra persistir em
 * dashboard_summary (consumidor da fila) e pro fallback do UC42 (calculado
 * na hora, sem gravar).
 */
public record DashboardStats(
        int trainingsThisMonth,
        int shotsThisMonth,
        List<String> practicedModalities,
        List<ModalityStats> modalityStats,
        String highlightResultTypeName,
        BigDecimal highlightValue
) {
}
