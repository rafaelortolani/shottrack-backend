package com.shottrack.backend.application.habituality;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0024: habitualidade (UC57) e meta pessoal (UC58) usam a data de início da visita; criar
 * visita retroativa ou editar as datas move a visita entre janelas e entre anos.
 */
class HabitualityVisitDatesTest extends HabitualityIntegrationTest {

    private UUID location;

    private UUID retroVisit(Instant startedAt) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trainingLocationId", location);
        body.put("visitType", "TREINO");
        body.put("startedAt", startedAt.toString());
        UUID id = idOf(send("POST", "/api/visits", athleteToken, body));
        send("POST", "/api/visits/" + id + "/closure", athleteToken, null).andExpect(status().isOk());
        return id;
    }

    private void edit(UUID visitId, Instant startedAt) throws Exception {
        send("PATCH", "/api/visits/" + visitId + "/dates", athleteToken, Map.of("startedAt", startedAt.toString()))
                .andExpect(status().isOk());
    }

    private static Instant noon(LocalDate day) {
        return day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC);
    }

    private void followSixMonthRule() throws Exception {
        UUID ruleId = createRule("Janela de 6 meses");
        createVersion(ruleId, version(LocalDate.of(2020, 1, 1), 6, level("Nível", 1, req("TREINO", 1))));
        track(athleteToken, ruleId, "Nível").andExpect(status().isOk());
        location = location(athleteToken, false);
    }

    private void assertDone(int done) throws Exception {
        progress(athleteToken).andExpect(jsonPath("$.data.requirements[0].done").value(done));
    }

    @Test
    void shouldCountRetroactiveVisitInsideTheWindowAndIgnoreOneOutside() throws Exception {
        followSixMonthRule();

        retroVisit(noon(today().minusMonths(2)));
        assertDone(1);
        retroVisit(noon(today().minusMonths(8)));   // aceita (≤ 12 meses), mas fora da janela de 6 meses
        assertDone(1);
    }

    @Test
    void shouldMoveVisitIntoAndOutOfTheWindowWhenEditingItsDate() throws Exception {
        followSixMonthRule();
        UUID visit = retroVisit(noon(today().minusMonths(8)));
        assertDone(0);

        edit(visit, noon(today().minusMonths(1)));
        assertDone(1);

        edit(visit, noon(today().minusMonths(9)));
        assertDone(0);
    }

    @Test
    void shouldHonorTheWindowEdgeWhenEditingTheVisitDate() throws Exception {
        followSixMonthRule();
        UUID visit = retroVisit(noon(today().minusMonths(2)));
        Instant periodStart = today().minusMonths(6).atStartOfDay().toInstant(ZoneOffset.UTC);

        edit(visit, periodStart);
        assertDone(1);
        edit(visit, periodStart.minusSeconds(1));
        assertDone(0);
    }

    @Test
    void shouldNotCountRetroactiveVisitStillInProgress() throws Exception {
        followSixMonthRule();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trainingLocationId", location);
        body.put("startedAt", noon(today().minusMonths(1)).toString());
        send("POST", "/api/visits", athleteToken, body).andExpect(status().isCreated());

        assertDone(0);
    }

    @Test
    void shouldMoveVisitBetweenYearsInThePersonalGoal() throws Exception {
        location = location(athleteToken, false);
        int year = today().getYear();
        send("PUT", "/api/habituality/personal-goal", athleteToken, Map.of("yearlyTrainings", 20)).andExpect(status().isOk());

        UUID visit = retroVisit(noon(LocalDate.of(year - 1, 12, 31)));
        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(0));

        edit(visit, LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(1));

        edit(visit, noon(LocalDate.of(year - 1, 12, 31)));
        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(0));
    }

    @Test
    void shouldCountRetroactiveVisitOfTheCurrentYearInThePersonalGoal() throws Exception {
        location = location(athleteToken, false);
        send("PUT", "/api/habituality/personal-goal", athleteToken, Map.of("yearlyTrainings", 20)).andExpect(status().isOk());

        retroVisit(LocalDate.of(today().getYear(), 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));

        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(1));
    }

    @Test
    void shouldKeepAnotherAthletesVisitOutOfMyWindowWhenMovingMine() throws Exception {
        followSixMonthRule();
        String other = registerAndLogin("outro.janela@shottrack.com");
        UUID otherLocation = location(other, false);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trainingLocationId", otherLocation);
        body.put("startedAt", noon(today().minusDays(3)).toString());
        UUID otherVisit = idOf(send("POST", "/api/visits", other, body));
        send("POST", "/api/visits/" + otherVisit + "/closure", other, null).andExpect(status().isOk());

        assertDone(0);
        send("PATCH", "/api/visits/" + otherVisit + "/dates", athleteToken, Map.of("startedAt", noon(today().minusDays(1)).toString()))
                .andExpect(status().isNotFound());
        assertDone(0);
    }
}
