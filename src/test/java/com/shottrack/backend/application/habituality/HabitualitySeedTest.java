package com.shottrack.backend.application.habituality;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V52: depois da migration só existe a regra em vigor (Decreto 11.615/2023). Os testes são
 * transacionais e desfazem tudo o que criam, então o que o banco mostra aqui é só o que a
 * migration deixou.
 */
class HabitualitySeedTest extends HabitualityIntegrationTest {

    private static final String RULE_NAME = "Habitualidade — Atirador Desportivo";

    /** A versão de 2023-12-01 da regra carregada pela migration. */
    private static final String SEED_VERSION = "(SELECT v.id FROM habituality_rule_versions v JOIN habituality_rules r ON r.id = v.rule_id "
            + "WHERE r.name = '" + RULE_NAME + "' AND v.valid_from = DATE '2023-12-01')";

    private record Row(String level, String type, int min, boolean club, boolean distinct, String scope) {
    }

    /**
     * Os testes rodam contra o banco de desenvolvimento, que pode ter acompanhamentos reais de
     * atletas e novas versões do admin depois da V52 — por isso as contagens são da regra e da
     * versão carregadas pela migration, não do banco inteiro.
     */
    @Test
    void shouldLeaveTheActiveRuleWithItsVersionThreeLevelsAndSevenRequirements() {
        assertThat(count("habituality_rules WHERE name = '" + RULE_NAME + "'")).isEqualTo(1);
        assertThat(count("habituality_rules WHERE name LIKE 'Regra %' OR name LIKE 'Probe%'")).isZero();
        assertThat(count("habituality_levels WHERE version_id = " + SEED_VERSION)).isEqualTo(3);
        assertThat(count("habituality_requirements WHERE level_id IN (SELECT id FROM habituality_levels WHERE version_id = "
                + SEED_VERSION + ")")).isEqualTo(7);

        Map<String, Object> rule = jdbcTemplate.queryForMap(
                "SELECT name, legal_reference, active, created_by FROM habituality_rules WHERE name = ?", RULE_NAME);
        assertThat(rule.get("legal_reference"))
                .isEqualTo("Decreto 11.615/2023, art. 35; Portaria COLOG/C Ex 166/2023, art. 95");
        assertThat(rule.get("active")).isEqualTo(true);
        assertThat(rule.get("created_by")).isNull();

        Map<String, Object> version = jdbcTemplate.queryForMap(
                "SELECT valid_from, window_months FROM habituality_rule_versions WHERE id = " + SEED_VERSION);
        assertThat(version.get("valid_from").toString()).isEqualTo("2023-12-01");
        assertThat(version.get("window_months")).isEqualTo(12);
    }

    @Test
    void shouldLoadLevelsInOrder() {
        List<Map<String, Object>> levels = jdbcTemplate.queryForList(
                "SELECT name, position FROM habituality_levels WHERE version_id = " + SEED_VERSION + " ORDER BY position");
        assertThat(levels).extracting(l -> l.get("name")).containsExactly("Nível I", "Nível II", "Nível III");
        assertThat(levels).extracting(l -> l.get("position")).containsExactly(1, 2, 3);
    }

    @Test
    void shouldLoadTheRequirementsOfEachLevel() {
        List<Row> rows = jdbcTemplate.query("""
                SELECT l.name AS level, r.activity_type, r.min_quantity, r.only_in_club, r.distinct_events, r.min_scope
                FROM habituality_requirements r JOIN habituality_levels l ON l.id = r.level_id
                WHERE l.version_id = %s
                ORDER BY l.position, r.activity_type, r.min_quantity
                """.formatted(SEED_VERSION), (rs, i) -> new Row(rs.getString("level"), rs.getString("activity_type"), rs.getInt("min_quantity"),
                rs.getBoolean("only_in_club"), rs.getBoolean("distinct_events"), rs.getString("min_scope")));

        assertThat(rows).containsExactlyInAnyOrder(
                new Row("Nível I", "QUALQUER", 8, true, true, null),
                new Row("Nível II", "TREINO", 12, true, false, null),
                new Row("Nível II", "COMPETICAO", 4, false, false, null),
                new Row("Nível II", "COMPETICAO", 2, false, false, "ESTADUAL"),
                new Row("Nível III", "TREINO", 20, true, false, null),
                new Row("Nível III", "COMPETICAO", 6, false, false, null),
                new Row("Nível III", "COMPETICAO", 2, false, false, "NACIONAL"));
    }

    @Test
    void shouldLeavePersonalGoalsUntouchedByTheMigrationTables() {
        // a migration só limpa regras/acompanhamentos: a tabela de metas continua existindo e utilizável
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM athlete_personal_goal", Integer.class)).isNotNull();
    }

    @Test
    void shouldExposeTheSeededRuleToAthletesWithoutAnyTestRule() throws Exception {
        var rules = data(send("GET", "/api/habituality/rules", athleteToken, null).andExpect(status().isOk()));

        var seeded = java.util.stream.StreamSupport.stream(rules.spliterator(), false)
                .filter(rule -> rule.get("name").asText().equals(RULE_NAME)).toList();
        assertThat(seeded).hasSize(1);
        assertThat(rules).allSatisfy(rule -> assertThat(rule.get("name").asText()).doesNotStartWith("Regra ").doesNotStartWith("Probe"));
        var rule = seeded.get(0);
        assertThat(rule.get("windowMonths").asInt()).isEqualTo(12);
        assertThat(rule.get("validFrom").asText()).isEqualTo("2023-12-01");
        assertThat(rule.get("levels")).hasSize(3);
        assertThat(rule.get("levels").get(0).get("name").asText()).isEqualTo("Nível I");
        assertThat(rule.get("levels").get(1).get("requirements")).hasSize(3);
        assertThat(rule.get("levels").get(2).get("requirements")).hasSize(3);
    }

    @Test
    void shouldCountCumulativeCompetitionRequirementsIndependently() throws Exception {
        var rules = data(send("GET", "/api/habituality/rules", athleteToken, null));
        String ruleId = java.util.stream.StreamSupport.stream(rules.spliterator(), false)
                .filter(rule -> rule.get("name").asText().equals(RULE_NAME)).findFirst().orElseThrow().get("id").asText();
        track(athleteToken, java.util.UUID.fromString(ruleId), "Nível II").andExpect(status().isOk());

        var location = location(athleteToken, false);
        // 4 competições, 2 delas com âmbito estadual ou superior (uma sem âmbito, uma nacional, uma estadual)
        closedVisit(athleteToken, location, "COMPETICAO", "ESTADUAL", today().minusDays(5));
        closedVisit(athleteToken, location, "COMPETICAO", "NACIONAL", today().minusDays(6));
        closedVisit(athleteToken, location, "COMPETICAO", null, today().minusDays(7));
        closedVisit(athleteToken, location, "COMPETICAO", null, today().minusDays(8));

        // (b) 4 competições: cumprida; (c) 2 com âmbito ≥ estadual: cumprida; (a) 12 treinos em clube: pendente
        progress(athleteToken)
                .andExpect(jsonPath("$.data.requirements[?(@.activityType=='COMPETICAO' && @.required==4)].done").value(4))
                .andExpect(jsonPath("$.data.requirements[?(@.activityType=='COMPETICAO' && @.required==4)].met").value(true))
                .andExpect(jsonPath("$.data.requirements[?(@.activityType=='COMPETICAO' && @.required==2)].done").value(2))
                .andExpect(jsonPath("$.data.requirements[?(@.activityType=='COMPETICAO' && @.required==2)].met").value(true))
                .andExpect(jsonPath("$.data.requirements[?(@.activityType=='TREINO')].met").value(false))
                .andExpect(jsonPath("$.data.compliant").value(false));
    }

    private int count(String table) {
        flushAndClear();
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
