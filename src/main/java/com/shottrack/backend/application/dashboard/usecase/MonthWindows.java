package com.shottrack.backend.application.dashboard.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * ADR-0022/UC42 v7: janelas dos indicadores do mês, em dias UTC (mesmo critério
 * de "treinos do mês" e da evolução). Atual: dia 1 do mês até hoje. Anterior: dia 1
 * do mês anterior até o MESMO dia do mês, limitado ao último dia daquele mês
 * (hoje = 31/03 → anterior = 01/02 a 28/02). Intervalos semiabertos [from, to).
 */
record MonthWindows(Instant currentFrom, Instant currentTo, Instant previousFrom, Instant previousTo) {

    static MonthWindows of(LocalDate today) {
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate previousFirstDay = firstDay.minusMonths(1);
        int previousLastDay = Math.min(today.getDayOfMonth(), previousFirstDay.lengthOfMonth());

        return new MonthWindows(
                startOf(firstDay),
                startOf(today.plusDays(1)),
                startOf(previousFirstDay),
                startOf(previousFirstDay.plusDays(previousLastDay)));
    }

    private static Instant startOf(LocalDate day) {
        return day.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
