package com.shottrack.backend.application.plan.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * ADR-0027: as chaves de limite são fixas no código (unidade, faixa e padrão); o banco guarda só os valores por plano.
 * Chave sem valor no banco usa {@link #defaultValue}. `enforced=false` = reservada: existe na lista, mas ninguém a aplica
 * nem o administrador a grava.
 */
@Getter
@RequiredArgsConstructor
public enum PlanLimit {

    PHOTOS_PER_SERIES("COUNT", 0, 20, 5, true),
    PHOTOS_PER_ATHLETE("COUNT", 0, 100_000, 500, true),
    STORAGE_BYTES_PER_ATHLETE("BYTES", 0, 107_374_182_400L, 536_870_912L, true),
    UPLOADS_PER_HOUR("PER_HOUR", 1, 1_000, 30, true),
    /** Reservada para a etapa B (ADR-0027); faixa e padrão ficam para o ADR da análise. */
    AI_ANALYSES_PER_MONTH("PER_MONTH", 0, 0, 0, false);

    private final String unit;
    private final long min;
    private final long max;
    private final long defaultValue;
    private final boolean enforced;

    public boolean accepts(long value) {
        return value >= min && value <= max;
    }

    public static Optional<PlanLimit> fromKey(String key) {
        return Arrays.stream(values()).filter(limit -> limit.name().equals(key)).findFirst();
    }
}
