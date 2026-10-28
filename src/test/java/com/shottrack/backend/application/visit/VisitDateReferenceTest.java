package com.shottrack.backend.application.visit;

import com.shottrack.backend.application.dashboard.usecase.DashboardRecalculationService;
import com.shottrack.backend.support.DashboardTestSupport;
import com.shottrack.backend.support.SeriesTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0024: a data de referência de todas as apurações é a data de INÍCIO DA VISITA —
 * dashboard (indicadores, recentes, recordes), evolução (UC46), histórico (UC47) e exportação
 * (UC53) — e não o horário em que o treino/série foi lançado.
 */
class VisitDateReferenceTest extends VisitDatesIntegrationTest {

    @Autowired
    private DashboardRecalculationService dashboardRecalculationService;

    private UUID tempo;
    private UUID ipsc;

    /** Visita com um treino IPSC, uma série de 10 disparos e Tempo 9.8. */
    private UUID visitWithSeries(Instant startedAt) throws Exception {
        UUID visitId = startVisit(startedAt);
        UUID trainingId = SeriesTestSupport.openTrainingInVisit(mockMvc, objectMapper, token, visitId, "IPSC");
        ipsc = DashboardTestSupport.modalityId(mockMvc, objectMapper, token, "IPSC");
        tempo = DashboardTestSupport.resultTypeId(mockMvc, objectMapper, token, "Tempo");
        DashboardTestSupport.configureResultType(mockMvc, objectMapper, token, ipsc, tempo);
        UUID seriesId = DashboardTestSupport.registerSeries(mockMvc, objectMapper, token, trainingId, 10);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"resultTypeId\":\"" + tempo + "\",\"value\":\"9.8\"}")).andExpect(status().isOk());
        return visitId;
    }

    private ResultActions dashboard() throws Exception {
        return mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token));
    }

    private ResultActions evolution(String period) throws Exception {
        return mockMvc.perform(get("/api/dashboard/evolution").header("Authorization", "Bearer " + token)
                .param("modalityId", ipsc.toString()).param("resultTypeId", tempo.toString())
                .param("period", period).param("mode", "MEDIA"));
    }

    private static Instant previousMonthDay1() {
        return noon(today().withDayOfMonth(1).minusMonths(1));
    }

    private static Instant currentMonthDay1() {
        return today().withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    // ---- dashboard ----

    @Test
    void shouldCountRetroactiveVisitInTheMonthOfItsDateNotTheMonthItWasEntered() throws Exception {
        visitWithSeries(previousMonthDay1());

        dashboard().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(1))
                .andExpect(jsonPath("$.data.shotsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth.previous").value(10));
    }

    @Test
    void shouldIgnoreRetroactiveVisitOlderThanThePreviousMonthInIndicators() throws Exception {
        visitWithSeries(noon(today().minusMonths(4)));

        dashboard().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth.previous").value(0));
    }

    @Test
    void shouldMoveVisitBetweenMonthsWhenEditingItsDate() throws Exception {
        UUID visitId = visitWithSeries(currentMonthDay1());
        dashboard().andExpect(jsonPath("$.data.trainingsThisMonth.value").value(1))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth.value").value(10));

        editDates(visitId, previousMonthDay1(), null).andExpect(status().isOk());
        dashboard().andExpect(jsonPath("$.data.trainingsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(1))
                .andExpect(jsonPath("$.data.shotsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth.previous").value(10));

        editDates(visitId, currentMonthDay1(), null).andExpect(status().isOk());
        dashboard().andExpect(jsonPath("$.data.trainingsThisMonth.value").value(1))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(0));
    }

    @Test
    void shouldKeepRecordsWhenVisitIsRetroactiveOrMoved() throws Exception {
        UUID visitId = visitWithSeries(noon(today().minusMonths(5)));

        dashboard().andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].value").value(9.8));

        editDates(visitId, noon(today().minusMonths(2)), null).andExpect(status().isOk());
        dashboard().andExpect(jsonPath("$.data.records[0].value").value(9.8));
    }

    @Test
    void shouldPersistTheSameResultAfterRecalculationOfARetroactiveVisit() throws Exception {
        visitWithSeries(noon(today().minusMonths(3)));
        UUID userId = userId();

        dashboardRecalculationService.recalculate(userId);

        dashboard().andExpect(jsonPath("$.data.records[0].value").value(9.8))
                .andExpect(jsonPath("$.data.trainingsThisMonth.value").value(0))
                .andExpect(jsonPath("$.data.trainingsThisMonth.previous").value(0));
    }

    @Test
    void shouldShowVisitDateOnRecentTrainingsAndOrderByIt() throws Exception {
        visitWithSeries(noon(today().minusMonths(3)));            // lançada primeiro, mas é a mais antiga
        Instant recentDay = noon(today().minusDays(2));
        visitWithSeries(recentDay);

        dashboard().andExpect(jsonPath("$.data.recentTrainings.length()").value(2))
                .andExpect(jsonPath("$.data.recentTrainings[0].startedAt").value(recentDay.toString()))
                .andExpect(jsonPath("$.data.recentTrainings[1].startedAt").value(noon(today().minusMonths(3)).toString()));
    }

    @Test
    void shouldReportOpenVisitStartDateOnMainAction() throws Exception {
        Instant start = noon(today().minusDays(4));
        startVisit(start);

        dashboard().andExpect(jsonPath("$.data.mainAction.activeVisit.startedAt").value(start.toString()));
    }

    // ---- evolução (UC46) ----

    @Test
    void shouldPlaceEvolutionPointOnTheVisitDate() throws Exception {
        LocalDate day = today().minusDays(3);
        visitWithSeries(noon(day));

        evolution("7d").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].date").value(day.toString()))
                .andExpect(jsonPath("$.data[0].value").value(9.8));
    }

    @Test
    void shouldMoveEvolutionPointInAndOutOfThePeriodWhenEditingTheVisitDate() throws Exception {
        UUID visitId = visitWithSeries(noon(today().minusDays(2)));
        evolution("7d").andExpect(jsonPath("$.data.length()").value(1));

        editDates(visitId, noon(today().minusDays(20)), null).andExpect(status().isOk());
        evolution("7d").andExpect(jsonPath("$.data.length()").value(0));
        evolution("30d").andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].date").value(today().minusDays(20).toString()));

        editDates(visitId, noon(today().minusDays(1)), null).andExpect(status().isOk());
        evolution("7d").andExpect(jsonPath("$.data[0].date").value(today().minusDays(1).toString()));
    }

    // ---- histórico (UC47) e exportação (UC53) ----

    @Test
    void shouldUseVisitDateInSeriesHistory() throws Exception {
        LocalDate day = today().minusDays(9);
        UUID visitId = visitWithSeries(noon(day));

        mockMvc.perform(get("/api/series").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].date").value(day.toString()));
        mockMvc.perform(get("/api/series").header("Authorization", "Bearer " + token)
                        .param("startDate", today().minusDays(5).toString()))
                .andExpect(jsonPath("$.data.length()").value(0));

        editDates(visitId, noon(today().minusDays(1)), null).andExpect(status().isOk());
        mockMvc.perform(get("/api/series").header("Authorization", "Bearer " + token)
                        .param("startDate", today().minusDays(5).toString()))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].date").value(today().minusDays(1).toString()));
    }

    @Test
    void shouldExportVisitStartDate() throws Exception {
        Instant start = noon(today().minusDays(6));
        startVisit(start);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users/me/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"senha12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visits[0].startedAt").value(start.toString()));
    }

    @Test
    void shouldOrderSeriesHistoryByVisitDateNotByEntryOrder() throws Exception {
        visitWithSeries(noon(today().minusDays(30)));
        visitWithSeries(noon(today().minusDays(2)));

        var result = mockMvc.perform(get("/api/series").header("Authorization", "Bearer " + token)).andReturn();
        var data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get(0).get("date").asText()).isEqualTo(today().minusDays(2).toString());
        assertThat(data.get(1).get("date").asText()).isEqualTo(today().minusDays(30).toString());
    }

    private UUID userId() throws Exception {
        var result = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }
}
