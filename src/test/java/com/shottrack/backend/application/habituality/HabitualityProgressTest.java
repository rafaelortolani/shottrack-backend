package com.shottrack.backend.application.habituality;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC57 — progresso de habitualidade. */
class HabitualityProgressTest extends HabitualityIntegrationTest {

    private UUID clubLocation;
    private UUID otherLocation;
    private LocalDate recent;

    /** Cria a regra (janela de 12 meses), faz o atleta acompanhá-la e prepara dois locais. */
    @SafeVarargs
    private void follow(java.util.Map<String, Object>... requirements) throws Exception {
        UUID ruleId = ruleWithLevel("Regra " + UUID.randomUUID(), "Nível", requirements);
        track(athleteToken, ruleId, "Nível").andExpect(status().isOk());
        clubLocation = location(athleteToken, true);
        otherLocation = location(athleteToken, false);
        recent = today().minusDays(10);
    }

    private ResultActions requirement0() throws Exception {
        return progress(athleteToken).andExpect(status().isOk());
    }

    private void assertRequirement(ResultActions result, int index, int required, int done, boolean met) throws Exception {
        result.andExpect(jsonPath("$.data.requirements[" + index + "].required").value(required))
                .andExpect(jsonPath("$.data.requirements[" + index + "].done").value(done))
                .andExpect(jsonPath("$.data.requirements[" + index + "].missing").value(Math.max(0, required - done)))
                .andExpect(jsonPath("$.data.requirements[" + index + "].met").value(met));
    }

    @Test
    void shouldReturnEmptyProgressWhenAthleteTracksNothing() throws Exception {
        progress(athleteToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rule").isEmpty())
                .andExpect(jsonPath("$.data.requirements").isEmpty())
                .andExpect(jsonPath("$.data.levelRequired").value(false))
                .andExpect(jsonPath("$.data.ruleInactive").value(false))
                .andExpect(jsonPath("$.data.personalGoal").isEmpty());
    }

    @Test
    void shouldReturnRuleLevelPeriodAndRequirementDetails() throws Exception {
        follow(req("TREINO", 3));
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);

        progress(athleteToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rule.name").isNotEmpty())
                .andExpect(jsonPath("$.data.rule.legalReference").value("Norma de teste"))
                .andExpect(jsonPath("$.data.level").value("Nível"))
                .andExpect(jsonPath("$.data.windowMonths").value(12))
                .andExpect(jsonPath("$.data.periodStart").value(today().minusMonths(12).toString()))
                .andExpect(jsonPath("$.data.periodEnd").value(today().toString()))
                .andExpect(jsonPath("$.data.compliant").value(false))
                .andExpect(jsonPath("$.data.requirements", hasSize(1)))
                .andExpect(jsonPath("$.data.requirements[0].description").value("Mínimo de 3 treino(s)"))
                .andExpect(jsonPath("$.data.requirements[0].activityType").value("TREINO"))
                .andExpect(jsonPath("$.data.upcomingChange").isEmpty());
        assertRequirement(requirement0(), 0, 3, 1, false);
    }

    @Test
    void shouldDescribeRequirementInEnglishWhenRequested() throws Exception {
        follow(req("COMPETICAO", 2, true, true, "NACIONAL"));

        mockMvc.perform(get("/api/habituality/progress")
                        .header("Authorization", "Bearer " + athleteToken)
                        .header("Accept-Language", "en"))
                .andExpect(jsonPath("$.data.requirements[0].description")
                        .value("At least 2 competition(s) (club only, distinct events, national scope or higher)"));
    }

    @Test
    void shouldCountOnlyTrainingForTrainingRequirement() throws Exception {
        follow(req("TREINO", 2));
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "ESTADUAL", recent);

        assertRequirement(requirement0(), 0, 2, 1, false);
    }

    @Test
    void shouldCountOnlyCompetitionForCompetitionRequirement() throws Exception {
        follow(req("COMPETICAO", 2));
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);
        closedVisit(athleteToken, otherLocation, "COMPETICAO", null, recent);
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "ESTADUAL", recent);

        assertRequirement(requirement0(), 0, 2, 2, true);
    }

    @Test
    void shouldCountBothTypesForAnyRequirement() throws Exception {
        follow(req("QUALQUER", 3));
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);
        closedVisit(athleteToken, otherLocation, "COMPETICAO", null, recent);

        assertRequirement(requirement0(), 0, 3, 2, false);
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent.minusDays(1));
        assertRequirement(requirement0(), 0, 3, 3, true);
    }

    @Test
    void shouldIgnoreVisitsStillInProgress() throws Exception {
        follow(req("TREINO", 1));
        openVisit(athleteToken, otherLocation, "TREINO", null);

        assertRequirement(requirement0(), 0, 1, 0, false);
        progress(athleteToken).andExpect(jsonPath("$.data.compliant").value(false));
    }

    @Test
    void shouldCountVisitOnceItIsClosed() throws Exception {
        follow(req("TREINO", 1));
        UUID visitId = openVisit(athleteToken, otherLocation, "TREINO", null);
        assertRequirement(requirement0(), 0, 1, 0, false);

        send("POST", "/api/visits/" + visitId + "/closure", athleteToken, null).andExpect(status().isOk());

        assertRequirement(requirement0(), 0, 1, 1, true);
    }

    @Test
    void shouldCountOnlyVisitsAtClubsForOnlyInClubFilter() throws Exception {
        follow(req("TREINO", 2, true, false, null));
        closedVisit(athleteToken, clubLocation, "TREINO", null, recent);
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);

        assertRequirement(requirement0(), 0, 2, 1, false);
    }

    @Test
    void shouldCountDistinctDatePlaceEventsForDistinctEventsFilter() throws Exception {
        follow(req("COMPETICAO", 3, false, true, null));
        closedVisit(athleteToken, clubLocation, "COMPETICAO", null, recent);
        closedVisit(athleteToken, clubLocation, "COMPETICAO", null, recent);          // mesma data e local: mesmo evento
        assertRequirement(requirement0(), 0, 3, 1, false);

        closedVisit(athleteToken, otherLocation, "COMPETICAO", null, recent);         // mesma data, outro local
        closedVisit(athleteToken, clubLocation, "COMPETICAO", null, recent.minusDays(1)); // mesmo local, outra data
        assertRequirement(requirement0(), 0, 3, 3, true);
    }

    @Test
    void shouldCountEveryVisitWhenDistinctEventsIsOff() throws Exception {
        follow(req("COMPETICAO", 2));
        closedVisit(athleteToken, clubLocation, "COMPETICAO", null, recent);
        closedVisit(athleteToken, clubLocation, "COMPETICAO", null, recent);

        assertRequirement(requirement0(), 0, 2, 2, true);
    }

    @Test
    void shouldCompareScopesInOrderAndIgnoreCompetitionWithoutScope() throws Exception {
        follow(req("COMPETICAO", 9, false, false, "NACIONAL"));
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "ESTADUAL", recent);       // abaixo do mínimo
        closedVisit(athleteToken, otherLocation, "COMPETICAO", null, recent);             // sem âmbito
        assertRequirement(requirement0(), 0, 9, 0, false);

        closedVisit(athleteToken, otherLocation, "COMPETICAO", "NACIONAL", recent);       // igual ao mínimo
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "INTERNACIONAL", recent);  // acima do mínimo
        assertRequirement(requirement0(), 0, 9, 2, false);
    }

    @Test
    void shouldAcceptEveryScopeWhenMinimumIsStateLevel() throws Exception {
        follow(req("COMPETICAO", 3, false, false, "ESTADUAL"));
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "ESTADUAL", recent);
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "NACIONAL", recent);
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "INTERNACIONAL", recent);

        assertRequirement(requirement0(), 0, 3, 3, true);
    }

    @Test
    void shouldRequireInternationalScopeWhenThatIsTheMinimum() throws Exception {
        follow(req("COMPETICAO", 1, false, false, "INTERNACIONAL"));
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "NACIONAL", recent);
        assertRequirement(requirement0(), 0, 1, 0, false);

        closedVisit(athleteToken, otherLocation, "COMPETICAO", "INTERNACIONAL", recent);
        assertRequirement(requirement0(), 0, 1, 1, true);
    }

    @Test
    void shouldNeverCountTrainingVisitsOnAnyTypeRequirementWithMinimumScope() throws Exception {
        follow(req("QUALQUER", 1, false, false, "ESTADUAL"));
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);

        assertRequirement(requirement0(), 0, 1, 0, false);
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "ESTADUAL", recent);
        assertRequirement(requirement0(), 0, 1, 1, true);
    }

    @Test
    void shouldCombineAllFiltersInOneRequirement() throws Exception {
        follow(req("COMPETICAO", 2, true, true, "NACIONAL"));
        closedVisit(athleteToken, clubLocation, "COMPETICAO", "NACIONAL", recent);
        closedVisit(athleteToken, clubLocation, "COMPETICAO", "NACIONAL", recent);                 // mesmo evento
        closedVisit(athleteToken, otherLocation, "COMPETICAO", "NACIONAL", recent);                // fora do clube
        closedVisit(athleteToken, clubLocation, "COMPETICAO", "ESTADUAL", recent.minusDays(2));    // âmbito baixo
        closedVisit(athleteToken, clubLocation, "TREINO", null, recent.minusDays(3));              // tipo errado
        assertRequirement(requirement0(), 0, 2, 1, false);

        closedVisit(athleteToken, clubLocation, "COMPETICAO", "INTERNACIONAL", recent.minusDays(4));
        assertRequirement(requirement0(), 0, 2, 2, true);
        progress(athleteToken).andExpect(jsonPath("$.data.compliant").value(true));
    }

    @Test
    void shouldRequireEveryRequirementOfTheLevelToBeCompliant() throws Exception {
        follow(req("TREINO", 2), req("COMPETICAO", 1, false, false, "ESTADUAL"));
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent.minusDays(1));

        ResultActions result = requirement0();
        assertRequirement(result, 0, 2, 2, true);
        assertRequirement(result, 1, 1, 0, false);
        result.andExpect(jsonPath("$.data.compliant").value(false));

        closedVisit(athleteToken, otherLocation, "COMPETICAO", "ESTADUAL", recent);
        progress(athleteToken).andExpect(jsonPath("$.data.compliant").value(true));
    }

    @Test
    void shouldCapMissingAtZeroWhenRequirementIsExceeded() throws Exception {
        follow(req("TREINO", 1));
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent);
        closedVisit(athleteToken, otherLocation, "TREINO", null, recent.minusDays(1));

        assertRequirement(requirement0(), 0, 1, 2, true);
    }

    @Test
    void shouldIncludeBothWindowEdgesAndExcludeTheDayBefore() throws Exception {
        follow(req("TREINO", 9));
        LocalDate periodStart = today().minusMonths(12);
        closedVisit(athleteToken, otherLocation, "TREINO", null, periodStart.minusDays(1)); // fora
        assertRequirement(requirement0(), 0, 9, 0, false);

        closedVisit(athleteToken, otherLocation, "TREINO", null, periodStart);              // primeiro dia da janela
        assertRequirement(requirement0(), 0, 9, 1, false);

        closedVisit(athleteToken, otherLocation, "TREINO", null, today());                  // hoje
        assertRequirement(requirement0(), 0, 9, 2, false);
    }

    @Test
    void shouldUseTheRulesWindowSize() throws Exception {
        UUID ruleId = createRule("Janela curta");
        createVersion(ruleId, version(LocalDate.of(2020, 1, 1), 3, level("Nível", 1, req("TREINO", 9))));
        track(athleteToken, ruleId, "Nível").andExpect(status().isOk());
        UUID location = location(athleteToken, false);
        closedVisit(athleteToken, location, "TREINO", null, today().minusMonths(3).minusDays(1)); // fora
        closedVisit(athleteToken, location, "TREINO", null, today().minusMonths(3));              // dentro
        closedVisit(athleteToken, location, "TREINO", null, today().minusMonths(2));              // dentro

        ResultActions result = requirement0();
        assertRequirement(result, 0, 9, 2, false);
        result.andExpect(jsonPath("$.data.windowMonths").value(3))
                .andExpect(jsonPath("$.data.periodStart").value(today().minusMonths(3).toString()));
    }

    @Test
    void shouldNotCountOtherAthletesVisits() throws Exception {
        follow(req("TREINO", 1));
        String other = registerAndLogin("outro.progresso@shottrack.com");
        closedVisit(other, location(other, true), "TREINO", null, recent);

        assertRequirement(requirement0(), 0, 1, 0, false);
    }

    @Test
    void shouldFlagInactiveRuleWithoutComputingProgress() throws Exception {
        UUID ruleId = ruleWithLevel("Vai inativar", "Nível", req("TREINO", 1));
        track(athleteToken, ruleId, "Nível").andExpect(status().isOk());
        closedVisit(athleteToken, location(athleteToken, false), "TREINO", null, today().minusDays(5));
        send("PUT", "/api/admin/habituality/rules/" + ruleId, adminToken, ruleBody("Vai inativar", false)).andExpect(status().isOk());

        progress(athleteToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleInactive").value(true))
                .andExpect(jsonPath("$.data.rule.name").value("Vai inativar"))
                .andExpect(jsonPath("$.data.level").value("Nível"))
                .andExpect(jsonPath("$.data.compliant").isEmpty())
                .andExpect(jsonPath("$.data.requirements").isEmpty());

        send("PUT", "/api/admin/habituality/rules/" + ruleId, adminToken, ruleBody("Vai inativar", true)).andExpect(status().isOk());
        progress(athleteToken)
                .andExpect(jsonPath("$.data.ruleInactive").value(false))
                .andExpect(jsonPath("$.data.compliant").value(true));
    }

    @Test
    void shouldAnnounceUpcomingChange() throws Exception {
        UUID ruleId = ruleWithLevel("Mudança", "Nível", req("TREINO", 1));
        LocalDate future = today().plusDays(20);
        createVersion(ruleId, version(future, 12, level("Nível", 1, req("TREINO", 5))));
        track(athleteToken, ruleId, "Nível").andExpect(status().isOk());

        progress(athleteToken)
                .andExpect(jsonPath("$.data.upcomingChange.validFrom").value(future.toString()))
                .andExpect(jsonPath("$.data.requirements[0].required").value(1));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        progress(null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
