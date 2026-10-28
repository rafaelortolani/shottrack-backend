package com.shottrack.backend.application.habituality;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC56 — escolher regra e nível. */
class HabitualityTrackingTest extends HabitualityIntegrationTest {

    private static final String RULES = "/api/habituality/rules";
    private static final String TRACKING = "/api/habituality/tracking";

    private UUID twoLevelRule(String name) throws Exception {
        UUID ruleId = createRule(name);
        createVersion(ruleId, version(LocalDate.of(2020, 1, 1), 12,
                level("Iniciante", 1, req("TREINO", 8)),
                level("Intermediário", 2, req("TREINO", 12), req("COMPETICAO", 2, false, true, "ESTADUAL"))));
        return ruleId;
    }

    @Test
    void shouldListActiveRulesWithCurrentVersionLevelsAndRequirements() throws Exception {
        UUID ruleId = twoLevelRule("Regra A");

        send("GET", RULES, athleteToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(ruleId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Regra A"))
                .andExpect(jsonPath("$.data[0].legalReference").value("Norma de teste"))
                .andExpect(jsonPath("$.data[0].validFrom").value("2020-01-01"))
                .andExpect(jsonPath("$.data[0].windowMonths").value(12))
                .andExpect(jsonPath("$.data[0].levels", hasSize(2)))
                .andExpect(jsonPath("$.data[0].levels[1].name").value("Intermediário"))
                .andExpect(jsonPath("$.data[0].levels[1].requirements[1].minScope").value("ESTADUAL"))
                .andExpect(jsonPath("$.data[0].upcomingVersion").isEmpty());
    }

    @Test
    void shouldNotListInactiveRulesNorRulesWithoutCurrentVersion() throws Exception {
        UUID inactive = twoLevelRule("Inativa");
        send("PUT", "/api/admin/habituality/rules/" + inactive, adminToken, ruleBody("Inativa", false)).andExpect(status().isOk());
        UUID futureOnly = createRule("So futura");
        createVersion(futureOnly, version(LocalDate.now().plusDays(10), 12, level("N", 1, req("TREINO", 1))));
        createRule("Sem versao");

        send("GET", RULES, athleteToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void shouldListCurrentVersionAndUpcomingVersionDate() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        LocalDate future = LocalDate.now().plusDays(45);
        createVersion(ruleId, version(future, 6, level("Novo", 1, req("TREINO", 1))));

        send("GET", RULES, athleteToken, null)
                .andExpect(jsonPath("$.data[0].validFrom").value("2020-01-01"))
                .andExpect(jsonPath("$.data[0].levels[0].name").value("Iniciante"))
                .andExpect(jsonPath("$.data[0].upcomingVersion.validFrom").value(future.toString()));
    }

    @Test
    void shouldTrackRuleAndLevelAndReplaceOnNewChoice() throws Exception {
        UUID first = twoLevelRule("Regra A");
        UUID second = ruleWithLevel("Regra B", "Avançado", req("TREINO", 20));

        track(athleteToken, first, "Iniciante")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleId").value(first.toString()))
                .andExpect(jsonPath("$.data.levelName").value("Iniciante"));
        track(athleteToken, first, "Intermediário").andExpect(status().isOk());
        track(athleteToken, second, "Avançado").andExpect(status().isOk());

        flushAndClear();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM athlete_habituality_tracking", Integer.class)).isEqualTo(1);
        progress(athleteToken)
                .andExpect(jsonPath("$.data.rule.id").value(second.toString()))
                .andExpect(jsonPath("$.data.level").value("Avançado"));
    }

    @Test
    void shouldMatchLevelNameIgnoringCaseAndStoreTheCanonicalName() throws Exception {
        UUID ruleId = twoLevelRule("Regra");

        track(athleteToken, ruleId, " intermediário ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.levelName").value("Intermediário"));
    }

    @Test
    void shouldStopTrackingAndBeIdempotent() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());

        send("DELETE", TRACKING, athleteToken, null).andExpect(status().isOk());
        progress(athleteToken).andExpect(jsonPath("$.data.rule").isEmpty());
        send("DELETE", TRACKING, athleteToken, null).andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnknownRule() throws Exception {
        track(athleteToken, UUID.randomUUID(), "Iniciante")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_RULE_NOT_FOUND"));
    }

    @Test
    void shouldRejectInactiveRule() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        send("PUT", "/api/admin/habituality/rules/" + ruleId, adminToken, ruleBody("Regra", false)).andExpect(status().isOk());

        track(athleteToken, ruleId, "Iniciante")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_RULE_INACTIVE"));
    }

    @Test
    void shouldRejectRuleWithoutCurrentVersion() throws Exception {
        UUID ruleId = createRule("So futura");
        createVersion(ruleId, version(LocalDate.now().plusDays(10), 12, level("N", 1, req("TREINO", 1))));

        track(athleteToken, ruleId, "N")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_NOT_FOUND"));
    }

    @Test
    void shouldRejectLevelThatDoesNotExistInCurrentVersion() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        createVersion(ruleId, version(LocalDate.now().plusDays(10), 12, level("Só na futura", 1, req("TREINO", 1))));

        track(athleteToken, ruleId, "Só na futura")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_LEVEL_NOT_FOUND"));
        track(athleteToken, ruleId, "Inexistente")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_LEVEL_NOT_FOUND"));
    }

    @Test
    void shouldKeepPreviousTrackingWhenNewChoiceFails() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());

        track(athleteToken, ruleId, "Inexistente").andExpect(status().isNotFound());

        progress(athleteToken).andExpect(jsonPath("$.data.level").value("Iniciante"));
    }

    @Test
    void shouldRejectInvalidBody() throws Exception {
        send("PUT", TRACKING, athleteToken, Map.of("levelName", "Iniciante"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        send("PUT", TRACKING, athleteToken, Map.of("ruleId", UUID.randomUUID(), "levelName", " "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldKeepLevelWhenNewVersionTakesEffectWithSameLevelName() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        UUID nextId = createVersion(ruleId, version(LocalDate.now().plusDays(10), 6,
                level("Iniciante", 1, req("TREINO", 3))));
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());
        progress(athleteToken)
                .andExpect(jsonPath("$.data.windowMonths").value(12))
                .andExpect(jsonPath("$.data.upcomingChange.validFrom").value(LocalDate.now().plusDays(10).toString()));

        setValidFrom(nextId, LocalDate.now().minusDays(1)); // a nova versão entra em vigor

        progress(athleteToken)
                .andExpect(jsonPath("$.data.levelRequired").value(false))
                .andExpect(jsonPath("$.data.level").value("Iniciante"))
                .andExpect(jsonPath("$.data.windowMonths").value(6))
                .andExpect(jsonPath("$.data.requirements[0].required").value(3))
                .andExpect(jsonPath("$.data.upcomingChange").isEmpty());
    }

    @Test
    void shouldRequireNewChoiceWhenNewVersionLacksTheLevelName() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        UUID nextId = createVersion(ruleId, version(LocalDate.now().plusDays(10), 12,
                level("Básico", 1, req("TREINO", 3))));
        track(athleteToken, ruleId, "Intermediário").andExpect(status().isOk());

        setValidFrom(nextId, LocalDate.now().minusDays(1));

        progress(athleteToken)
                .andExpect(jsonPath("$.data.levelRequired").value(true))
                .andExpect(jsonPath("$.data.level").value("Intermediário"))
                .andExpect(jsonPath("$.data.compliant").isEmpty())
                .andExpect(jsonPath("$.data.requirements").isEmpty());

        track(athleteToken, ruleId, "Básico").andExpect(status().isOk());
        progress(athleteToken)
                .andExpect(jsonPath("$.data.levelRequired").value(false))
                .andExpect(jsonPath("$.data.level").value("Básico"));
    }

    @Test
    void shouldIsolateTrackingBetweenAthletes() throws Exception {
        UUID ruleId = twoLevelRule("Regra");
        String other = registerAndLogin("outro.atleta.habitualidade@shottrack.com");
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());

        progress(other).andExpect(jsonPath("$.data.rule").isEmpty());
        track(other, ruleId, "Intermediário").andExpect(status().isOk());
        send("DELETE", TRACKING, other, null).andExpect(status().isOk());

        progress(athleteToken)
                .andExpect(jsonPath("$.data.rule.id").value(ruleId.toString()))
                .andExpect(jsonPath("$.data.level").value("Iniciante"));
    }

    @Test
    void shouldRejectEveryAthleteRouteWithoutToken() throws Exception {
        for (String[] route : new String[][]{{"GET", RULES}, {"PUT", TRACKING}, {"DELETE", TRACKING}}) {
            send(route[0], route[1], null, Map.of("ruleId", UUID.randomUUID(), "levelName", "X"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }
}
