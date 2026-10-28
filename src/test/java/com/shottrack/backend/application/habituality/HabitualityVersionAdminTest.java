package com.shottrack.backend.application.habituality;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC55 — versões, níveis e exigências. */
class HabitualityVersionAdminTest extends HabitualityIntegrationTest {

    private static final String BASE = "/api/admin/habituality";

    private Map<String, Object> twoLevels(LocalDate validFrom) {
        return version(validFrom, 12,
                level("Iniciante", 1, req("TREINO", 8)),
                level("Intermediário", 2, req("TREINO", 12), req("COMPETICAO", 2, false, true, "ESTADUAL")));
    }

    private int count(String table, String where, Object... args) {
        flushAndClear();
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE " + where, Integer.class, args);
    }

    @Test
    void shouldCreateFirstVersionWithTwoLevelsInThePast() throws Exception {
        UUID ruleId = createRule("Regra");

        postVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ruleId").value(ruleId.toString()))
                .andExpect(jsonPath("$.data.validFrom").value("2020-01-01"))
                .andExpect(jsonPath("$.data.windowMonths").value(12))
                .andExpect(jsonPath("$.data.current").value(true))
                .andExpect(jsonPath("$.data.removable").value(false))
                .andExpect(jsonPath("$.data.levels", hasSize(2)))
                .andExpect(jsonPath("$.data.levels[0].name").value("Iniciante"))
                .andExpect(jsonPath("$.data.levels[1].requirements", hasSize(2)))
                .andExpect(jsonPath("$.data.levels[1].requirements[1].activityType").value("COMPETICAO"))
                .andExpect(jsonPath("$.data.levels[1].requirements[1].distinctEvents").value(true))
                .andExpect(jsonPath("$.data.levels[1].requirements[1].minScope").value("ESTADUAL"));
    }

    @Test
    void shouldCreateFirstVersionDatedToday() throws Exception {
        UUID ruleId = createRule("Regra hoje");

        postVersion(ruleId, twoLevels(today()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.current").value(true));
    }

    @Test
    void shouldCreateFutureVersionAndListWithCurrentMarked() throws Exception {
        UUID ruleId = createRule("Regra");
        createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        postVersion(ruleId, twoLevels(today().plusDays(30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.current").value(false))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andExpect(jsonPath("$.data.removable").value(true));

        send("GET", BASE + "/rules/" + ruleId + "/versions", adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].validFrom").value("2020-01-01"))
                .andExpect(jsonPath("$.data[0].current").value(true))
                .andExpect(jsonPath("$.data[1].current").value(false));
    }

    @Test
    void shouldEditFutureVersionReplacingLevelsAndRequirements() throws Exception {
        UUID ruleId = createRule("Regra");
        createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        UUID futureId = createVersion(ruleId, twoLevels(today().plusDays(30)));

        send("PUT", BASE + "/versions/" + futureId, adminToken,
                version(today().plusDays(60), 6, level("Único", 1, req("QUALQUER", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validFrom").value(today().plusDays(60).toString()))
                .andExpect(jsonPath("$.data.windowMonths").value(6))
                .andExpect(jsonPath("$.data.levels", hasSize(1)))
                .andExpect(jsonPath("$.data.levels[0].name").value("Único"));

        // níveis e exigências antigos foram substituídos, não acumulados
        org.assertj.core.api.Assertions.assertThat(count("habituality_levels", "version_id = ?", futureId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(count("habituality_requirements",
                "level_id IN (SELECT id FROM habituality_levels WHERE version_id = ?)", futureId)).isEqualTo(1);
    }

    @Test
    void shouldRemoveFutureVersionWithItsLevelsAndRequirements() throws Exception {
        UUID ruleId = createRule("Regra");
        createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        UUID futureId = createVersion(ruleId, twoLevels(today().plusDays(30)));

        send("DELETE", BASE + "/versions/" + futureId, adminToken, null).andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(count("habituality_rule_versions", "id = ?", futureId)).isZero();
        org.assertj.core.api.Assertions.assertThat(count("habituality_levels", "version_id = ?", futureId)).isZero();
        send("GET", BASE + "/rules/" + ruleId + "/versions", adminToken, null).andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void shouldEditCurrentVersionWhenNoAthleteTracksTheRule() throws Exception {
        UUID ruleId = createRule("Regra");
        UUID currentId = createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));

        send("PUT", BASE + "/versions/" + currentId, adminToken,
                version(LocalDate.of(2020, 1, 1), 24, level("Corrigido", 1, req("TREINO", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.windowMonths").value(24))
                .andExpect(jsonPath("$.data.levels[0].name").value("Corrigido"));
    }

    @Test
    void shouldEditCurrentVersionMovingTheFirstVersionDateIntoThePast() throws Exception {
        UUID ruleId = createRule("Regra");
        UUID currentId = createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));

        send("PUT", BASE + "/versions/" + currentId, adminToken, twoLevels(LocalDate.of(2019, 6, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validFrom").value("2019-06-01"));
    }

    @Test
    void shouldRejectEditingCurrentVersionWhenAnAthleteTracksTheRule() throws Exception {
        UUID ruleId = createRule("Regra");
        UUID currentId = createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());

        send("PUT", BASE + "/versions/" + currentId, adminToken, twoLevels(LocalDate.of(2020, 1, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_NOT_EDITABLE"));
    }

    @Test
    void shouldRejectEditingPastVersionWhenAnAthleteTracksTheRule() throws Exception {
        UUID ruleId = createRule("Regra");
        UUID pastId = createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        UUID secondId = createVersion(ruleId, twoLevels(today().plusDays(10)));
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());
        setValidFrom(secondId, today().minusDays(1));

        // a primeira passou a ser "passada" (a segunda está em vigor)
        send("PUT", BASE + "/versions/" + pastId, adminToken, twoLevels(LocalDate.of(2020, 1, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_NOT_EDITABLE"));
    }

    @Test
    void shouldStillEditFutureVersionWhenAnAthleteTracksTheRule() throws Exception {
        UUID ruleId = createRule("Regra");
        createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        UUID futureId = createVersion(ruleId, twoLevels(today().plusDays(30)));
        track(athleteToken, ruleId, "Iniciante").andExpect(status().isOk());

        send("PUT", BASE + "/versions/" + futureId, adminToken, twoLevels(today().plusDays(31)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldNeverRemoveCurrentVersion() throws Exception {
        UUID ruleId = createRule("Regra");
        UUID currentId = createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));

        send("DELETE", BASE + "/versions/" + currentId, adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_NOT_EDITABLE"));
    }

    @Test
    void shouldNeverRemovePastVersion() throws Exception {
        UUID ruleId = createRule("Regra");
        UUID pastId = createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        UUID secondId = createVersion(ruleId, twoLevels(today().plusDays(10)));
        setValidFrom(secondId, today().minusDays(1));

        send("DELETE", BASE + "/versions/" + pastId, adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_NOT_EDITABLE"));
    }

    @Test
    void shouldRejectSecondVersionWithPastOrTodayDate() throws Exception {
        UUID ruleId = createRule("Regra");
        createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));

        for (LocalDate invalid : List.of(LocalDate.of(2021, 1, 1), today())) {
            postVersion(ruleId, twoLevels(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_DATE_INVALID"));
        }
    }

    @Test
    void shouldRejectDuplicateDate() throws Exception {
        UUID ruleId = createRule("Regra");
        createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        createVersion(ruleId, twoLevels(today().plusDays(30)));

        postVersion(ruleId, twoLevels(today().plusDays(30)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_DATE_INVALID"));
    }

    @Test
    void shouldRejectMovingFutureVersionToPastOrToAnotherVersionsDate() throws Exception {
        UUID ruleId = createRule("Regra");
        createVersion(ruleId, twoLevels(LocalDate.of(2020, 1, 1)));
        UUID futureA = createVersion(ruleId, twoLevels(today().plusDays(30)));
        createVersion(ruleId, twoLevels(today().plusDays(60)));

        send("PUT", BASE + "/versions/" + futureA, adminToken, twoLevels(today().minusDays(5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_DATE_INVALID"));
        send("PUT", BASE + "/versions/" + futureA, adminToken, twoLevels(today().plusDays(60)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_DATE_INVALID"));
    }

    @Test
    void shouldRejectDuplicateLevelNamesIgnoringCase() throws Exception {
        UUID ruleId = createRule("Regra");

        postVersion(ruleId, version(LocalDate.of(2020, 1, 1), 12,
                level("Iniciante", 1, req("TREINO", 1)), level("iniciante", 2, req("TREINO", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectInvalidBodies() throws Exception {
        UUID ruleId = createRule("Regra");
        LocalDate date = LocalDate.of(2020, 1, 1);

        // sem níveis
        postVersion(ruleId, version(date, 12)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        // nível sem exigência
        postVersion(ruleId, version(date, 12, level("Vazio", 1))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        // minQuantity 0
        postVersion(ruleId, version(date, 12, level("N", 1, req("TREINO", 0)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        // windowMonths 0
        postVersion(ruleId, version(date, 0, level("N", 1, req("TREINO", 1)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        // nome de nível em branco
        postVersion(ruleId, version(date, 12, level(" ", 1, req("TREINO", 1)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        // tipo de atividade inválido
        postVersion(ruleId, version(date, 12, level("N", 1, req("OUTRO", 1)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        // âmbito inválido
        postVersion(ruleId, version(date, 12, level("N", 1, req("COMPETICAO", 1, false, false, "MUNDIAL"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMinScopeOnTrainingRequirementButAcceptOnCompetitionAndAny() throws Exception {
        UUID ruleId = createRule("Regra");

        postVersion(ruleId, version(LocalDate.of(2020, 1, 1), 12,
                level("N", 1, req("TREINO", 1, false, false, "ESTADUAL"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        postVersion(ruleId, version(LocalDate.of(2020, 1, 1), 12,
                level("N", 1, req("COMPETICAO", 1, false, false, "NACIONAL"), req("QUALQUER", 1, false, false, "ESTADUAL"))))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldNotPersistAnythingWhenVersionIsInvalid() throws Exception {
        UUID ruleId = createRule("Regra");

        postVersion(ruleId, version(LocalDate.of(2020, 1, 1), 12,
                level("A", 1, req("TREINO", 1)), level("a", 2, req("TREINO", 1))))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(count("habituality_rule_versions", "rule_id = ?", ruleId)).isZero();
    }

    @Test
    void shouldRejectUnknownRuleAndVersion() throws Exception {
        UUID unknown = UUID.randomUUID();
        Map<String, Object> body = twoLevels(today().plusDays(10));

        send("GET", BASE + "/rules/" + unknown + "/versions", adminToken, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_RULE_NOT_FOUND"));
        postVersion(unknown, body).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_RULE_NOT_FOUND"));
        send("PUT", BASE + "/versions/" + unknown, adminToken, body).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_NOT_FOUND"));
        send("DELETE", BASE + "/versions/" + unknown, adminToken, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_VERSION_NOT_FOUND"));
    }

    @Test
    void shouldRejectEveryVersionRouteWithoutToken() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = twoLevels(today().plusDays(10));
        for (String[] route : new String[][]{
                {"GET", BASE + "/rules/" + id + "/versions"}, {"POST", BASE + "/rules/" + id + "/versions"},
                {"PUT", BASE + "/versions/" + id}, {"DELETE", BASE + "/versions/" + id}}) {
            send(route[0], route[1], null, body)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void shouldForbidEveryVersionRouteForCommonAthlete() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = twoLevels(today().plusDays(10));
        for (String[] route : new String[][]{
                {"GET", BASE + "/rules/" + id + "/versions"}, {"POST", BASE + "/rules/" + id + "/versions"},
                {"PUT", BASE + "/versions/" + id}, {"DELETE", BASE + "/versions/" + id}}) {
            send(route[0], route[1], athleteToken, body)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }
}
