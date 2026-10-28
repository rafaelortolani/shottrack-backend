package com.shottrack.backend.application.dashboard.usecase;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC42 v7: janelas dos indicadores do mês — os casos de calendário (virada de
 * mês, mês anterior mais curto, janeiro) não dependem da data de hoje, então
 * são testados direto sobre a função pura.
 */
class MonthWindowsTest {

    @Test
    void shouldCompareMidMonthAgainstSameDayOfPreviousMonth() {
        MonthWindows windows = MonthWindows.of(LocalDate.of(2026, 6, 15));

        assertWindow(windows, "2026-06-01", "2026-06-16", "2026-05-01", "2026-05-16");
    }

    @Test
    void shouldCapPreviousWindowAtLastDayOfShorterPreviousMonth() {
        // 31/03 → fevereiro só vai até 28
        assertWindow(MonthWindows.of(LocalDate.of(2026, 3, 31)), "2026-03-01", "2026-04-01", "2026-02-01", "2026-03-01");
        // 30/03 em ano bissexto → fevereiro vai até 29
        assertWindow(MonthWindows.of(LocalDate.of(2024, 3, 30)), "2024-03-01", "2024-03-31", "2024-02-01", "2024-03-01");
        // 31/05 → abril só vai até 30
        assertWindow(MonthWindows.of(LocalDate.of(2026, 5, 31)), "2026-05-01", "2026-06-01", "2026-04-01", "2026-05-01");
    }

    @Test
    void shouldCrossYearBoundaryInJanuary() {
        assertWindow(MonthWindows.of(LocalDate.of(2026, 1, 10)), "2026-01-01", "2026-01-11", "2025-12-01", "2025-12-11");
    }

    @Test
    void shouldCompareFirstDayOfMonthAgainstFirstDayOfPreviousMonth() {
        assertWindow(MonthWindows.of(LocalDate.of(2026, 3, 1)), "2026-03-01", "2026-03-02", "2026-02-01", "2026-02-02");
    }

    private void assertWindow(MonthWindows windows, String currentFrom, String currentTo, String previousFrom,
                              String previousTo) {
        assertThat(windows.currentFrom()).isEqualTo(instant(currentFrom));
        assertThat(windows.currentTo()).isEqualTo(instant(currentTo));
        assertThat(windows.previousFrom()).isEqualTo(instant(previousFrom));
        assertThat(windows.previousTo()).isEqualTo(instant(previousTo));
    }

    private java.time.Instant instant(String day) {
        return LocalDate.parse(day).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
