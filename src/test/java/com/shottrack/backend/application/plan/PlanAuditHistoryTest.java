package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC67: histórico de alterações de planos (mais recente primeiro; contas excluídas anonimizadas). */
class PlanAuditHistoryTest extends PlanApiTest {

    @Test
    void shouldListTheLimitChangeAndThePlanAssignmentNewestFirst() throws Exception {
        String admin = admin();
        String email = athleteEmail("historico");
        athlete(email);
        createPlan(admin, "PAID", "Pago");
        call(HttpMethod.PATCH, "/api/admin/plans/FREE/limits", admin, Map.of("limits", Map.of("PHOTOS_PER_SERIES", 7))).andExpect(status().isOk());
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "PAID")).andExpect(status().isOk());

        JsonNode history = data(call(HttpMethod.GET, "/api/admin/plans/audit", admin, null).andExpect(status().isOk()));

        assertThat(history).hasSize(3);
        assertThat(history.get(0).get("action").asText()).isEqualTo("PLAN_ASSIGNED");
        assertThat(history.get(0).get("targetUser").get("email").asText()).isEqualTo(email);
        assertThat(history.get(0).get("oldValue").asText()).isEqualTo("FREE");
        assertThat(history.get(0).get("newValue").asText()).isEqualTo("PAID");
        assertThat(history.get(0).get("actor").get("deleted").asBoolean()).isFalse();
        assertThat(history.get(0).get("actor").get("name").asText()).isEqualTo("Admin Plano");
        assertThat(history.get(1).get("action").asText()).isEqualTo("LIMIT_CHANGED");
        assertThat(history.get(1).get("targetPlanCode").asText()).isEqualTo("FREE");
        assertThat(history.get(1).get("field").asText()).isEqualTo("PHOTOS_PER_SERIES");
        assertThat(history.get(1).get("oldValue").asText()).isEqualTo("5");
        assertThat(history.get(1).get("newValue").asText()).isEqualTo("7");
        assertThat(history.get(2).get("action").asText()).isEqualTo("PLAN_CREATED");
    }

    @Test
    void shouldHonourTheLimitParameter() throws Exception {
        String admin = admin();
        createPlan(admin, "PLANO_A", "Plano A");
        createPlan(admin, "PLANO_B", "Plano B");
        createPlan(admin, "PLANO_C", "Plano C");

        assertThat(data(call(HttpMethod.GET, "/api/admin/plans/audit?limit=2", admin, null))).hasSize(2);
        assertThat(data(call(HttpMethod.GET, "/api/admin/plans/audit?limit=200", admin, null))).hasSize(3);
    }

    @Test
    void shouldRefuseALimitOutOfRange() throws Exception {
        String admin = admin();
        for (String limit : new String[]{"0", "-1", "201", "abc"}) {
            call(HttpMethod.GET, "/api/admin/plans/audit?limit=" + limit, admin, null).andExpect(status().isBadRequest());
        }
        call(HttpMethod.GET, "/api/admin/plans/audit?limit=0", admin, null).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldNotExposeSensitiveData() throws Exception {
        String admin = admin();
        String email = athleteEmail("sem.segredo");
        athlete(email);
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "FREE")).andExpect(status().isOk());
        createPlan(admin, "PAID", "Pago");
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "PAID")).andExpect(status().isOk());

        String body = call(HttpMethod.GET, "/api/admin/plans/audit", admin, null).andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("password").doesNotContain("Hash").doesNotContain("token");
    }

    @Test
    void shouldRefuseARequestWithoutTokenAndFromAnAthlete() throws Exception {
        call(HttpMethod.GET, "/api/admin/plans/audit", null, null).andExpect(status().isUnauthorized());
        call(HttpMethod.GET, "/api/admin/plans/audit", athlete(), null).andExpect(status().isForbidden());
    }
}
