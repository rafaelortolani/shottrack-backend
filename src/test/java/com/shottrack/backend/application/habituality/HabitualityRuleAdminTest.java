package com.shottrack.backend.application.habituality;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC54 — gerenciar regras de habitualidade. */
class HabitualityRuleAdminTest extends HabitualityIntegrationTest {

    private static final String RULES = "/api/admin/habituality/rules";

    @Test
    void shouldCreateRule() throws Exception {
        send("POST", RULES, adminToken, ruleBody("Regra A", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Regra A"))
                .andExpect(jsonPath("$.data.legalReference").value("Norma de teste"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldCreateActiveRuleWhenActiveIsOmitted() throws Exception {
        send("POST", RULES, adminToken, Map.of("name", "Sem flag"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldListRulesOrderedByName() throws Exception {
        createRule("Zeta");
        createRule("Alfa");

        // só as regras deste teste e a da migration V52 entram na checagem: o que mais houver no
        // banco (de outros testes/processos) não pode alterar o resultado
        var names = new java.util.ArrayList<String>();
        data(send("GET", RULES, adminToken, null).andExpect(status().isOk()))
                .forEach(rule -> names.add(rule.get("name").asText()));

        org.assertj.core.api.Assertions.assertThat(names)
                .filteredOn(name -> name.equals("Alfa") || name.equals("Zeta")
                        || name.equals("Habitualidade — Atirador Desportivo"))
                .containsExactly("Alfa", "Habitualidade — Atirador Desportivo", "Zeta");
    }

    @Test
    void shouldEditRuleAndDeactivateAndReactivate() throws Exception {
        UUID id = createRule("Original");

        send("PUT", RULES + "/" + id, adminToken, Map.of("name", "Renomeada", "legalReference", "Nova norma", "active", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renomeada"))
                .andExpect(jsonPath("$.data.legalReference").value("Nova norma"))
                .andExpect(jsonPath("$.data.active").value(false));

        send("PUT", RULES + "/" + id, adminToken, Map.of("name", "Renomeada", "active", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldAllowEditingRuleKeepingItsOwnName() throws Exception {
        UUID id = createRule("Mesmo nome");

        send("PUT", RULES + "/" + id, adminToken, Map.of("name", "Mesmo nome", "legalReference", "Outra"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRecordAuditOnCreateAndUpdate() throws Exception {
        UUID id = createRule("Auditada");
        UUID adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, ADMIN_EMAIL);

        send("PUT", RULES + "/" + id, adminToken, Map.of("name", "Auditada 2")).andExpect(status().isOk());
        flushAndClear();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT created_by, updated_by FROM habituality_rules WHERE id = ?", id);
        org.assertj.core.api.Assertions.assertThat(row.get("created_by")).isEqualTo(adminId);
        org.assertj.core.api.Assertions.assertThat(row.get("updated_by")).isEqualTo(adminId);
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        send("POST", RULES, adminToken, Map.of("name", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectDuplicateNameIgnoringCase() throws Exception {
        createRule("Duplicada");

        send("POST", RULES, adminToken, Map.of("name", "DUPLICADA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRenameToAnotherRulesName() throws Exception {
        createRule("Primeira");
        UUID second = createRule("Segunda");

        send("PUT", RULES + "/" + second, adminToken, Map.of("name", "Primeira"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectUnknownRule() throws Exception {
        send("PUT", RULES + "/" + UUID.randomUUID(), adminToken, Map.of("name", "X"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_RULE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Regra de habitualidade não encontrada"));
    }

    @Test
    void shouldTranslateErrorToEnglish() throws Exception {
        mockMvc.perform(get(RULES + "/" + UUID.randomUUID() + "/versions")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HABITUALITY_RULE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Habituality rule not found"));
    }

    @Test
    void shouldRejectEveryRuleRouteWithoutToken() throws Exception {
        UUID id = UUID.randomUUID();
        for (String[] route : new String[][]{{"GET", RULES}, {"POST", RULES}, {"PUT", RULES + "/" + id}}) {
            send(route[0], route[1], null, Map.of("name", "X"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void shouldForbidEveryRuleRouteForCommonAthlete() throws Exception {
        UUID id = UUID.randomUUID();
        for (String[] route : new String[][]{{"GET", RULES}, {"POST", RULES}, {"PUT", RULES + "/" + id}}) {
            send(route[0], route[1], athleteToken, Map.of("name", "X"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }
}
