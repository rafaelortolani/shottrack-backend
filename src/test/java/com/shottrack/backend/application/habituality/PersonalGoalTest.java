package com.shottrack.backend.application.habituality;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC58 — meta pessoal de treinos no ano civil. */
class PersonalGoalTest extends HabitualityIntegrationTest {

    private static final String GOAL = "/api/habituality/personal-goal";

    private ResultActions goal(String token, Object value) throws Exception {
        return send("PUT", GOAL, token, Map.of("yearlyTrainings", value));
    }

    @Test
    void shouldSetGoalAndShowItOnProgress() throws Exception {
        goal(athleteToken, 50)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.yearlyTrainings").value(50))
                .andExpect(jsonPath("$.data.doneThisYear").value(0));

        progress(athleteToken)
                .andExpect(jsonPath("$.data.rule").isEmpty())
                .andExpect(jsonPath("$.data.personalGoal.yearlyTrainings").value(50))
                .andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(0));
    }

    @Test
    void shouldChangeGoalKeepingASingleRow() throws Exception {
        goal(athleteToken, 50).andExpect(status().isOk());
        goal(athleteToken, 80).andExpect(status().isOk()).andExpect(jsonPath("$.data.yearlyTrainings").value(80));

        flushAndClear();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM athlete_personal_goal WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                Integer.class, "atleta.habitualidade@shottrack.com")).isEqualTo(1);
    }

    @Test
    void shouldRemoveGoalAndBeIdempotent() throws Exception {
        goal(athleteToken, 50).andExpect(status().isOk());

        send("DELETE", GOAL, athleteToken, null).andExpect(status().isOk());
        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal").isEmpty());
        send("DELETE", GOAL, athleteToken, null).andExpect(status().isOk());
    }

    @Test
    void shouldAcceptBoundaryValues() throws Exception {
        goal(athleteToken, 1).andExpect(status().isOk());
        goal(athleteToken, 1000).andExpect(status().isOk());
    }

    @Test
    void shouldRejectValuesOutsideTheAllowedRange() throws Exception {
        for (Object invalid : new Object[]{0, -3, 1001}) {
            goal(athleteToken, invalid)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        send("PUT", GOAL, athleteToken, Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        goal(athleteToken, "muitos").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldCountOnlyClosedTrainingVisitsOfTheCurrentYear() throws Exception {
        UUID location = location(athleteToken, false);
        int year = today().getYear();
        closedVisit(athleteToken, location, "TREINO", null, LocalDate.of(year, 1, 1));       // 1º de janeiro: conta
        closedVisit(athleteToken, location, "TREINO", null, today());                          // hoje: conta
        closedVisit(athleteToken, location, "TREINO", null, LocalDate.of(year - 1, 12, 31)); // ano anterior: não conta
        closedVisit(athleteToken, location, "COMPETICAO", null, today());                      // competição: não conta
        openVisit(athleteToken, location, "TREINO", null);                                     // em andamento: não conta

        goal(athleteToken, 10).andExpect(jsonPath("$.data.doneThisYear").value(2));
        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(2));
    }

    @Test
    void shouldRestartCountWhenTheYearTurns() throws Exception {
        UUID location = location(athleteToken, false);
        int year = today().getYear();
        // "virada de ano": as visitas do ano passado deixam de contar, as de 31/12 e 1º/1 separam os anos
        closedVisit(athleteToken, location, "TREINO", null, LocalDate.of(year - 1, 12, 31));
        closedVisit(athleteToken, location, "TREINO", null, LocalDate.of(year - 1, 6, 1));
        goal(athleteToken, 10).andExpect(jsonPath("$.data.doneThisYear").value(0));

        closedVisit(athleteToken, location, "TREINO", null, LocalDate.of(year, 1, 1));
        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(1));
    }

    @Test
    void shouldIsolateGoalBetweenAthletes() throws Exception {
        String other = registerAndLogin("outro.meta@shottrack.com");
        goal(athleteToken, 30).andExpect(status().isOk());
        closedVisit(athleteToken, location(athleteToken, false), "TREINO", null, today());

        progress(other).andExpect(jsonPath("$.data.personalGoal").isEmpty());
        goal(other, 5).andExpect(jsonPath("$.data.doneThisYear").value(0));
        send("DELETE", GOAL, other, null).andExpect(status().isOk());

        progress(athleteToken)
                .andExpect(jsonPath("$.data.personalGoal.yearlyTrainings").value(30))
                .andExpect(jsonPath("$.data.personalGoal.doneThisYear").value(1));
    }

    @Test
    void shouldWorkIndependentlyOfRuleAndLevel() throws Exception {
        UUID ruleId = ruleWithLevel("Regra", "Iniciante", req("TREINO", 100));
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());
        goal(athleteToken, 12).andExpect(status().isOk());

        progress(athleteToken)
                .andExpect(jsonPath("$.data.compliant").value(false))
                .andExpect(jsonPath("$.data.personalGoal.yearlyTrainings").value(12));
        send("DELETE", "/api/habituality/tracking", athleteToken, null).andExpect(status().isOk());
        progress(athleteToken).andExpect(jsonPath("$.data.personalGoal.yearlyTrainings").value(12));
    }

    @Test
    void shouldRejectBothRoutesWithoutToken() throws Exception {
        send("PUT", GOAL, null, Map.of("yearlyTrainings", 10)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        send("DELETE", GOAL, null, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
