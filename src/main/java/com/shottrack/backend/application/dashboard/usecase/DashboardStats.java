package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.model.ModalityRecord;
import com.shottrack.backend.application.dashboard.model.ModalityStats;
import com.shottrack.backend.application.dashboard.model.ResultRecord;

import java.util.List;

/**
 * Resultado do recálculo (ADR-0015) — mesma forma usada pra persistir em
 * dashboard_summary (consumidor da fila) e pro fallback do UC42 (calculado
 * na hora, sem gravar). Só o que depende do histórico inteiro; indicadores do
 * mês são sob demanda (ADR-0022).
 */
public record DashboardStats(
        List<ModalityStats> modalityStats,
        List<ModalityRecord> modalityRecords,
        List<ResultRecord> records
) {
}
