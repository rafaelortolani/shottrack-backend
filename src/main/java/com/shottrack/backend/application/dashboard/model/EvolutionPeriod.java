package com.shottrack.backend.application.dashboard.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

/**
 * UC46/ADR-0016: janela da evolução, sempre terminando hoje (inclusive).
 */
public enum EvolutionPeriod {
    LAST_7_DAYS("7d", Period.ofDays(7)),
    LAST_30_DAYS("30d", Period.ofDays(30)),
    LAST_3_MONTHS("3m", Period.ofMonths(3)),
    LAST_YEAR("1a", Period.ofYears(1));

    private final String code;
    private final Period length;

    EvolutionPeriod(String code, Period length) {
        this.code = code;
        this.length = length;
    }

    public static EvolutionPeriod fromCode(String code) {
        return Arrays.stream(values())
                .filter(period -> period.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Período desconhecido: " + code));
    }

    /**
     * Primeiro dia da janela: "7d" com hoje = dia 10 cobre os dias 4 a 10.
     */
    public LocalDate firstDay(LocalDate today) {
        return today.minus(length).plusDays(1);
    }
}
