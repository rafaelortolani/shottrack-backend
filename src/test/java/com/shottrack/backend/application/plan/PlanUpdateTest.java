package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC69: editar plano (nome, ativo, padrão) — sempre exatamente um plano padrão e ativo. */
class PlanUpdateTest extends PlanApiTest {

    private Map<String, Object> body(Object name, Boolean active, Boolean isDefault) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) {
            body.put("name", name);
        }
        if (active != null) {
            body.put("active", active);
        }
        if (isDefault != null) {
            body.put("default", isDefault);
        }
        return body;
    }

    private ResultActionsHolder patch(String token, String code, Map<String, Object> body) throws Exception {
        return new ResultActionsHolder(call(HttpMethod.PATCH, "/api/admin/plans/" + code, token, body));
    }

    private record ResultActionsHolder(org.springframework.test.web.servlet.ResultActions actions) {
    }

    private List<String> defaultCodes() {
        return jdbc.queryForList("SELECT code FROM plans WHERE is_default", String.class);
    }

    @Test
    void shouldRenameAPlanAndAudit() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");

        JsonNode updated = data(patch(admin, "PAID", body("Pago mensal", null, null)).actions().andExpect(status().isOk()));

        assertThat(updated.get("name").asText()).isEqualTo("Pago mensal");
        var row = jdbc.queryForMap("SELECT * FROM plan_audit WHERE action = 'PLAN_UPDATED'");
        assertThat(row.get("field_key")).isEqualTo("name");
        assertThat(row.get("old_value")).isEqualTo("Pago");
        assertThat(row.get("new_value")).isEqualTo("Pago mensal");
    }

    @Test
    void shouldDeactivateAndReactivateANonDefaultPlan() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");

        assertThat(data(patch(admin, "PAID", body(null, false, null)).actions().andExpect(status().isOk())).get("active").asBoolean()).isFalse();
        assertThat(data(patch(admin, "PAID", body(null, true, null)).actions().andExpect(status().isOk())).get("active").asBoolean()).isTrue();
        assertThat(auditCount("PLAN_UPDATED")).isEqualTo(2);
    }

    @Test
    void shouldMakeAnotherActivePlanTheDefaultAndUnmarkThePreviousOne() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");

        JsonNode updated = data(patch(admin, "PAID", body(null, null, true)).actions().andExpect(status().isOk()));

        assertThat(updated.get("default").asBoolean()).isTrue();
        assertThat(defaultCodes()).containsExactly("PAID");
        assertThat(jdbc.queryForObject("SELECT is_default FROM plans WHERE code = 'FREE'", Boolean.class)).isFalse();
        assertThat(auditCount("PLAN_UPDATED")).as("o plano novo e o que deixou de ser padrão").isEqualTo(2);
    }

    @Test
    void shouldGiveTheNewDefaultToNewUsers() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");
        patch(admin, "PAID", body(null, null, true)).actions().andExpect(status().isOk());

        String email = athleteEmail("novo.padrao");
        athlete(email);

        assertThat(jdbc.queryForObject("SELECT p.code FROM users u JOIN plans p ON p.id = u.plan_id WHERE u.email = ?", String.class, email)).isEqualTo("PAID");
    }

    @Test
    void shouldAnswerPlanNotFound() throws Exception {
        patch(admin(), "NOPE", body("Novo nome", null, null)).actions()
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    void shouldRefuseAnInvalidOrDuplicatedName() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");

        patch(admin, "PAID", body("A", null, null)).actions()
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PLAN_NAME_INVALID"));
        patch(admin, "PAID", body("GRATUITO", null, null)).actions()
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_NAME_ALREADY_EXISTS"));
        assertThat(auditCount("PLAN_UPDATED")).isZero();
    }

    @Test
    void shouldRefuseToDeactivateTheDefaultPlan() throws Exception {
        patch(admin(), "FREE", body(null, false, null)).actions()
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_DEFAULT_CANNOT_BE_DEACTIVATED"));

        assertThat(jdbc.queryForObject("SELECT active FROM plans WHERE code = 'FREE'", Boolean.class)).isTrue();
    }

    @Test
    void shouldRefuseToMakeAnInactivePlanTheDefault() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");
        patch(admin, "PAID", body(null, false, null)).actions().andExpect(status().isOk());

        patch(admin, "PAID", body(null, null, true)).actions()
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_NOT_ACTIVE"));
        // desativar e tornar padrão no mesmo pedido também é recusado
        createPlan(admin, "BETA", "Beta");
        patch(admin, "BETA", body(null, false, true)).actions()
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_NOT_ACTIVE"));
        assertThat(defaultCodes()).containsExactly("FREE");
    }

    @Test
    void shouldRefuseToRemoveTheDefaultWithoutIndicatingAnother() throws Exception {
        patch(admin(), "FREE", body(null, null, false)).actions()
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_DEFAULT_REQUIRED"));

        assertThat(defaultCodes()).containsExactly("FREE");
    }

    @Test
    void shouldAcceptDefaultFalseOnAPlanThatIsNotTheDefault() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");

        patch(admin, "PAID", body(null, null, false)).actions().andExpect(status().isOk());

        assertThat(auditCount("PLAN_UPDATED")).isZero();
    }

    @Test
    void shouldRefuseARequestWithNoField() throws Exception {
        patch(admin(), "FREE", new HashMap<>()).actions()
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldNotAuditWhenNothingChanges() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");
        long before = auditCount();

        patch(admin, "PAID", body("Pago", true, false)).actions().andExpect(status().isOk());
        patch(admin, "FREE", body(null, true, true)).actions().andExpect(status().isOk());

        assertThat(auditCount()).isEqualTo(before);
    }

    @Test
    void shouldKeepPlanAndLimitsOfAthletesWhenThePlanIsDeactivated() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");
        call(HttpMethod.PATCH, "/api/admin/plans/PAID/limits", admin, Map.of("limits", Map.of("PHOTOS_PER_SERIES", 12))).andExpect(status().isOk());
        String email = athleteEmail("plano.inativo");
        String token = athlete(email);
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "PAID")).andExpect(status().isOk());

        patch(admin, "PAID", body(null, false, null)).actions().andExpect(status().isOk());

        JsonNode mine = data(call(HttpMethod.GET, "/api/users/me/plan", token, null).andExpect(status().isOk()));
        assertThat(mine.get("plan").get("code").asText()).isEqualTo("PAID");
        assertThat(mine.get("limits").get("photosPerSeries").asLong()).isEqualTo(12);
    }

    @Test
    void shouldRefuseARequestWithoutToken() throws Exception {
        patch(null, "FREE", body("Novo", null, null)).actions().andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefuseAnAthleteWhoIsNotAdmin() throws Exception {
        patch(athlete(), "FREE", body("Novo", null, null)).actions().andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT name FROM plans WHERE code = 'FREE'", String.class)).isEqualTo("Gratuito");
    }
}
