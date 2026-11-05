package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC52 (auditoria anonimizada na exclusão da conta) e UC53 (exportação traz o nome do plano). */
class PlanLgpdTest extends PlanApiTest {

    private void deleteAccount(String token) throws Exception {
        call(HttpMethod.POST, "/api/users/me/deletion", token, Map.of("password", PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void shouldKeepAuditRowsWithAnonymizedTargetWhenTheAthleteDeletesTheAccount() throws Exception {
        String admin = admin();
        String email = athleteEmail("exclui.alvo");
        String token = athlete(email);
        UUID athleteId = userId(email);
        createPlan(admin, "PAID", "Pago");
        call(HttpMethod.PUT, "/api/admin/users/" + athleteId + "/plan", admin, Map.of("planCode", "PAID")).andExpect(status().isOk());

        deleteAccount(token);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Long.class, athleteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_audit WHERE action = 'PLAN_ASSIGNED'", Long.class)).as("o registro permanece").isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT target_user_id FROM plan_audit WHERE action = 'PLAN_ASSIGNED'", UUID.class)).as("alvo anonimizado").isNull();
        JsonNode history = data(call(HttpMethod.GET, "/api/admin/plans/audit", admin, null));
        assertThat(history.get(0).get("action").asText()).isEqualTo("PLAN_ASSIGNED");
        assertThat(history.get(0).get("targetUser").get("deleted").asBoolean()).isTrue();
        assertThat(history.get(0).get("targetUser").get("email").isNull()).isTrue();
    }

    @Test
    void shouldKeepAuditRowsWithAnonymizedActorWhenTheAdminDeletesTheAccount() throws Exception {
        String leavingAdmin = admin();
        createPlan(leavingAdmin, "PAID", "Pago");
        call(HttpMethod.PATCH, "/api/admin/plans/PAID/limits", leavingAdmin, Map.of("limits", Map.of("PHOTOS_PER_SERIES", 9))).andExpect(status().isOk());
        String stayingAdmin = admin();

        deleteAccount(leavingAdmin);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_audit WHERE actor_user_id IS NULL", Long.class)).isEqualTo(2);
        JsonNode history = data(call(HttpMethod.GET, "/api/admin/plans/audit", stayingAdmin, null));
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("actor").get("deleted").asBoolean()).isTrue();
        assertThat(history.get(0).get("actor").get("name").isNull()).isTrue();
        assertThat(history.get(0).get("field").asText()).isEqualTo("PHOTOS_PER_SERIES");
    }

    @Test
    void shouldNotTouchTheAuditRowsOfOtherUsers() throws Exception {
        String admin = admin();
        String email = athleteEmail("outro.alvo");
        athlete(email);
        createPlan(admin, "PAID", "Pago");
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "PAID")).andExpect(status().isOk());
        String other = athlete();

        deleteAccount(other);

        assertThat(jdbc.queryForObject("SELECT target_user_id FROM plan_audit WHERE action = 'PLAN_ASSIGNED'", UUID.class)).isEqualTo(userId(email));
    }

    @Test
    void shouldIncludeThePlanNameInTheDataExportWithoutTheInternalId() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago mensal");
        String email = athleteEmail("exporta.plano");
        String token = athlete(email);
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "PAID")).andExpect(status().isOk());

        String body = call(HttpMethod.POST, "/api/users/me/export", token, Map.of("currentPassword", PASSWORD))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JsonNode profile = objectMapper.readTree(body).get("profile");
        assertThat(profile.get("plan").asText()).isEqualTo("Pago mensal");
        assertThat(body).doesNotContain(planId("PAID").toString()).doesNotContain("PAID");
    }
}
