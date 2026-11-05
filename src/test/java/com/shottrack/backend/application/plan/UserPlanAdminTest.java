package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC65: localizar atleta por e-mail e alterar o plano dele. */
class UserPlanAdminTest extends PlanApiTest {

    private String planPath(UUID userId) {
        return "/api/admin/users/" + userId + "/plan";
    }

    private String planOf(String email) {
        return jdbc.queryForObject("SELECT p.code FROM users u JOIN plans p ON p.id = u.plan_id WHERE u.email = ?", String.class, email);
    }

    @Test
    void shouldAssignAnActivePlanAndAudit() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");
        String email = athleteEmail("troca.plano");
        athlete(email);

        JsonNode response = data(call(HttpMethod.PUT, planPath(userId(email)), admin, Map.of("planCode", "PAID")).andExpect(status().isOk()));

        assertThat(response.get("planCode").asText()).isEqualTo("PAID");
        assertThat(response.get("email").asText()).isEqualTo(email);
        assertThat(planOf(email)).isEqualTo("PAID");
        assertThat(auditCount("PLAN_ASSIGNED")).isEqualTo(1);
        var row = jdbc.queryForMap("SELECT * FROM plan_audit WHERE action = 'PLAN_ASSIGNED'");
        assertThat(row.get("target_type")).isEqualTo("USER");
        assertThat(row.get("target_user_id")).isEqualTo(userId(email));
        assertThat(row.get("old_value")).isEqualTo("FREE");
        assertThat(row.get("new_value")).isEqualTo("PAID");
        assertThat(row.get("actor_user_id")).isNotNull();
    }

    @Test
    void shouldNotAuditNorFailWhenAssigningTheSamePlan() throws Exception {
        String admin = admin();
        String email = athleteEmail("mesmo.plano");
        athlete(email);

        call(HttpMethod.PUT, planPath(userId(email)), admin, Map.of("planCode", "FREE")).andExpect(status().isOk());

        assertThat(auditCount()).isZero();
    }

    @Test
    void shouldAnswerUserNotFound() throws Exception {
        call(HttpMethod.PUT, planPath(UUID.randomUUID()), admin(), Map.of("planCode", "FREE"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void shouldAnswerPlanNotFound() throws Exception {
        String email = athleteEmail("sem.plano");
        athlete(email);

        call(HttpMethod.PUT, planPath(userId(email)), admin(), Map.of("planCode", "NOPE"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    void shouldRefuseAnInactivePlan() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");
        call(HttpMethod.PATCH, "/api/admin/plans/PAID", admin, Map.of("active", false)).andExpect(status().isOk());
        String email = athleteEmail("plano.inativo");
        athlete(email);

        call(HttpMethod.PUT, planPath(userId(email)), admin, Map.of("planCode", "PAID"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PLAN_NOT_ACTIVE"));
        assertThat(planOf(email)).isEqualTo("FREE");
        assertThat(auditCount("PLAN_ASSIGNED")).isZero();
    }

    @Test
    void shouldRefuseAMissingPlanCode() throws Exception {
        String email = athleteEmail("sem.codigo");
        athlete(email);

        call(HttpMethod.PUT, planPath(userId(email)), admin(), "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldPreserveTheDataOfAnAthleteWhoseUsageExceedsTheNewPlan() throws Exception {
        String admin = admin();
        createPlan(admin, "SMALL", "Pequeno");
        call(HttpMethod.PATCH, "/api/admin/plans/SMALL/limits", admin, Map.of("limits", Map.of("PHOTOS_PER_ATHLETE", 0, "PHOTOS_PER_SERIES", 0)))
                .andExpect(status().isOk());
        String email = athleteEmail("uso.acima");
        String token = athlete(email);
        UUID athleteId = userId(email);
        UUID training = com.shottrack.backend.support.SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID series = PlanEnforcementTest.createSeries(mockMvc, objectMapper, token, training);
        PlanEnforcementTest.upload(mockMvc, token, series).andExpect(status().isCreated());
        PlanEnforcementTest.upload(mockMvc, token, series).andExpect(status().isCreated());

        call(HttpMethod.PUT, planPath(athleteId), admin, Map.of("planCode", "SMALL")).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_photos WHERE athlete_id = ?", Long.class, athleteId)).as("nada apagado").isEqualTo(2);
        PlanEnforcementTest.upload(mockMvc, token, series).andExpect(status().isConflict());
    }

    @Test
    void shouldSearchByEmailFragmentWithoutExposingSensitiveData() throws Exception {
        String admin = admin();
        String email = athleteEmail("busca.especifica");
        athlete(email);

        String body = call(HttpMethod.GET, "/api/admin/users?email=busca.especifica", admin, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode found = objectMapper.readTree(body).get("data");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).get("email").asText()).isEqualTo(email);
        assertThat(found.get(0).get("planCode").asText()).isEqualTo("FREE");
        assertThat(found.get(0).fieldNames()).toIterable().containsExactlyInAnyOrder("id", "name", "email", "planCode", "planName", "photoCount", "storageBytes");
        assertThat(body).doesNotContain("passwordHash").doesNotContain("password").doesNotContain("role").doesNotContain("token");
    }

    @Test
    void shouldSearchCaseInsensitivelyAndTreatWildcardsAsLiterals() throws Exception {
        String admin = admin();
        athlete(athleteEmail("misto.caso"));

        assertThat(data(call(HttpMethod.GET, "/api/admin/users?email=MISTO.caso", admin, null))).hasSize(1);
        assertThat(data(call(HttpMethod.GET, "/api/admin/users?email=%%%", admin, null))).as("% é literal, não curinga").isEmpty();
        assertThat(data(call(HttpMethod.GET, "/api/admin/users?email=___", admin, null))).as("_ é literal, não curinga").isEmpty();
    }

    @Test
    void shouldRefuseASearchWithFewerThanThreeCharactersOrNone() throws Exception {
        String admin = admin();
        call(HttpMethod.GET, "/api/admin/users?email=ab", admin, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        call(HttpMethod.GET, "/api/admin/users", admin, null).andExpect(status().isBadRequest());
        call(HttpMethod.GET, "/api/admin/users?email=   ", admin, null).andExpect(status().isBadRequest());
    }

    @Test
    void shouldLimitTheNumberOfSearchResults() throws Exception {
        String admin = admin();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 12; i++) {
            athlete("lote." + tag + "." + i + "@shottrack.com");
        }

        assertThat(data(call(HttpMethod.GET, "/api/admin/users?email=lote." + tag, admin, null))).hasSize(10);
    }

    @Test
    void shouldRefuseRequestsWithoutTokenAndFromAthletes() throws Exception {
        String email = athleteEmail("alvo");
        String token = athlete(email);

        call(HttpMethod.GET, "/api/admin/users?email=alvo", null, null).andExpect(status().isUnauthorized());
        call(HttpMethod.PUT, planPath(userId(email)), null, Map.of("planCode", "FREE")).andExpect(status().isUnauthorized());
        call(HttpMethod.GET, "/api/admin/users?email=alvo", token, null).andExpect(status().isForbidden());
        call(HttpMethod.PUT, planPath(userId(email)), token, Map.of("planCode", "FREE")).andExpect(status().isForbidden());
    }
}
