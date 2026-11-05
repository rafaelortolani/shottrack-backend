package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC64: edição parcial dos limites de um plano, com auditoria e tudo-ou-nada. */
class PlanLimitsEditTest extends PlanApiTest {

    private static final String PATH = "/api/admin/plans/FREE/limits";

    private Map<String, Object> body(Object limits) {
        return Map.of("limits", limits);
    }

    private long stored(String key) {
        return jdbc.queryForObject("SELECT value FROM plan_limits WHERE plan_id = ? AND limit_key = ?", Long.class, planId("FREE"), key);
    }

    @Test
    void shouldEditOneLimitAndAuditOldAndNewValue() throws Exception {
        String admin = admin();

        JsonNode plan = data(call(HttpMethod.PATCH, PATH, admin, body(Map.of("PHOTOS_PER_SERIES", 8))).andExpect(status().isOk()));

        assertThat(PlanListTest.limit(plan, "PHOTOS_PER_SERIES").get("value").asLong()).isEqualTo(8);
        assertThat(stored("PHOTOS_PER_SERIES")).isEqualTo(8);
        assertThat(auditCount("LIMIT_CHANGED")).isEqualTo(1);
        var row = jdbc.queryForMap("SELECT * FROM plan_audit WHERE action = 'LIMIT_CHANGED'");
        assertThat(row.get("target_plan_code")).isEqualTo("FREE");
        assertThat(row.get("field_key")).isEqualTo("PHOTOS_PER_SERIES");
        assertThat(row.get("old_value")).isEqualTo("5");
        assertThat(row.get("new_value")).isEqualTo("8");
        assertThat(row.get("actor_user_id")).isNotNull();
    }

    @Test
    void shouldEditSeveralLimitsAtOnce() throws Exception {
        String admin = admin();

        call(HttpMethod.PATCH, PATH, admin, body(Map.of("PHOTOS_PER_SERIES", 8, "STORAGE_BYTES_PER_ATHLETE", 1_073_741_824L)))
                .andExpect(status().isOk());

        assertThat(stored("PHOTOS_PER_SERIES")).isEqualTo(8);
        assertThat(stored("STORAGE_BYTES_PER_ATHLETE")).isEqualTo(1_073_741_824L);
        assertThat(auditCount("LIMIT_CHANGED")).isEqualTo(2);
    }

    @Test
    void shouldNotAuditWhenTheValueDoesNotChange() throws Exception {
        String admin = admin();

        call(HttpMethod.PATCH, PATH, admin, body(Map.of("PHOTOS_PER_SERIES", 5, "UPLOADS_PER_HOUR", 30))).andExpect(status().isOk());

        assertThat(auditCount("LIMIT_CHANGED")).isZero();
    }

    @Test
    void shouldAuditOnlyTheLimitsThatChanged() throws Exception {
        String admin = admin();

        call(HttpMethod.PATCH, PATH, admin, body(Map.of("PHOTOS_PER_SERIES", 5, "UPLOADS_PER_HOUR", 40))).andExpect(status().isOk());

        assertThat(auditCount("LIMIT_CHANGED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT field_key FROM plan_audit WHERE action = 'LIMIT_CHANGED'", String.class)).isEqualTo("UPLOADS_PER_HOUR");
    }

    @Test
    void shouldAcceptTheBoundaryValues() throws Exception {
        String admin = admin();

        call(HttpMethod.PATCH, PATH, admin, body(Map.of("PHOTOS_PER_SERIES", 0, "PHOTOS_PER_ATHLETE", 100_000,
                "UPLOADS_PER_HOUR", 1, "STORAGE_BYTES_PER_ATHLETE", 107_374_182_400L))).andExpect(status().isOk());
    }

    @Test
    void shouldAnswerPlanNotFound() throws Exception {
        call(HttpMethod.PATCH, "/api/admin/plans/NOPE/limits", admin(), body(Map.of("PHOTOS_PER_SERIES", 8)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    void shouldRefuseAnUnknownKey() throws Exception {
        call(HttpMethod.PATCH, PATH, admin(), body(Map.of("MADE_UP", 8)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PLAN_LIMIT_INVALID_KEY"));
    }

    @Test
    void shouldRefuseTheReservedKey() throws Exception {
        call(HttpMethod.PATCH, PATH, admin(), body(Map.of("AI_ANALYSES_PER_MONTH", 10)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PLAN_LIMIT_INVALID_KEY"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_limits WHERE limit_key = 'AI_ANALYSES_PER_MONTH'", Long.class)).isZero();
    }

    @Test
    void shouldRefuseValuesOutOfRangeNegativeTextOrDecimal() throws Exception {
        String admin = admin();
        for (String json : new String[]{
                "{\"limits\":{\"PHOTOS_PER_SERIES\":21}}",
                "{\"limits\":{\"PHOTOS_PER_SERIES\":-1}}",
                "{\"limits\":{\"UPLOADS_PER_HOUR\":0}}",
                "{\"limits\":{\"UPLOADS_PER_HOUR\":1001}}",
                "{\"limits\":{\"PHOTOS_PER_ATHLETE\":100001}}",
                "{\"limits\":{\"STORAGE_BYTES_PER_ATHLETE\":107374182401}}",
                "{\"limits\":{\"PHOTOS_PER_SERIES\":\"oito\"}}",
                "{\"limits\":{\"PHOTOS_PER_SERIES\":\"8\"}}",
                "{\"limits\":{\"PHOTOS_PER_SERIES\":8.5}}",
                "{\"limits\":{\"PHOTOS_PER_SERIES\":null}}",
                "{\"limits\":{\"PHOTOS_PER_SERIES\":true}}",
                "{\"limits\":{\"PHOTOS_PER_SERIES\":99999999999999999999999}}"}) {
            call(HttpMethod.PATCH, PATH, admin, json)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PLAN_LIMIT_OUT_OF_RANGE"));
        }
        assertThat(auditCount()).isZero();
    }

    @Test
    void shouldWriteNothingWhenOneOfTheLimitsIsInvalid() throws Exception {
        String admin = admin();
        Map<String, Object> limits = new HashMap<>();
        limits.put("PHOTOS_PER_SERIES", 9);
        limits.put("UPLOADS_PER_HOUR", 5000);

        call(HttpMethod.PATCH, PATH, admin, body(limits)).andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT value FROM plan_limits WHERE plan_id = ? AND limit_key = 'PHOTOS_PER_SERIES'", Long.class, planId("FREE")))
                .as("o limite válido do mesmo pedido não foi gravado").isEqualTo(5);
        assertThat(auditCount()).isZero();
    }

    @Test
    void shouldRefuseAnEmptyOrMissingLimitsObject() throws Exception {
        String admin = admin();
        call(HttpMethod.PATCH, PATH, admin, "{\"limits\":{}}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        call(HttpMethod.PATCH, PATH, admin, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRefuseARequestWithoutToken() throws Exception {
        call(HttpMethod.PATCH, PATH, null, body(Map.of("PHOTOS_PER_SERIES", 8))).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefuseAnAthleteWhoIsNotAdmin() throws Exception {
        call(HttpMethod.PATCH, PATH, athlete(), body(Map.of("PHOTOS_PER_SERIES", 8)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat(auditCount()).isZero();
    }
}
