package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC63: lista de planos e limites (administrador). */
class PlanListTest extends PlanApiTest {

    @Test
    void shouldListTheFreePlanWithTheFiveKeysAndTheReservedOneNotEnforced() throws Exception {
        String admin = admin();

        JsonNode plans = data(call(HttpMethod.GET, "/api/admin/plans", admin, null).andExpect(status().isOk()));

        JsonNode free = null;
        for (JsonNode plan : plans) {
            if ("FREE".equals(plan.get("code").asText())) {
                free = plan;
            }
        }
        assertThat(free).isNotNull();
        assertThat(free.get("name").asText()).isEqualTo("Gratuito");
        assertThat(free.get("active").asBoolean()).isTrue();
        assertThat(free.get("default").asBoolean()).isTrue();
        assertThat(free.get("limits")).hasSize(5);

        JsonNode photosPerSeries = limit(free, "PHOTOS_PER_SERIES");
        assertThat(photosPerSeries.get("value").asLong()).isEqualTo(5);
        assertThat(photosPerSeries.get("unit").asText()).isEqualTo("COUNT");
        assertThat(photosPerSeries.get("min").asLong()).isZero();
        assertThat(photosPerSeries.get("max").asLong()).isEqualTo(20);
        assertThat(photosPerSeries.get("defaultValue").asLong()).isEqualTo(5);
        assertThat(photosPerSeries.get("enforced").asBoolean()).isTrue();
        assertThat(photosPerSeries.get("customized").asBoolean()).isFalse();
        assertThat(limit(free, "PHOTOS_PER_ATHLETE").get("value").asLong()).isEqualTo(500);
        assertThat(limit(free, "STORAGE_BYTES_PER_ATHLETE").get("value").asLong()).isEqualTo(536_870_912L);
        assertThat(limit(free, "UPLOADS_PER_HOUR").get("value").asLong()).isEqualTo(30);
        assertThat(limit(free, "AI_ANALYSES_PER_MONTH").get("enforced").asBoolean()).isFalse();
    }

    @Test
    void shouldCountTheAthletesOfEachPlan() throws Exception {
        String admin = admin();
        long before = athletes(admin, "FREE");
        athlete();
        athlete();

        assertThat(athletes(admin, "FREE")).isEqualTo(before + 2);
    }

    @Test
    void shouldMarkACustomizedLimit() throws Exception {
        String admin = admin();
        call(HttpMethod.PATCH, "/api/admin/plans/FREE/limits", admin, java.util.Map.of("limits", java.util.Map.of("PHOTOS_PER_SERIES", 8)))
                .andExpect(status().isOk());

        JsonNode plans = data(call(HttpMethod.GET, "/api/admin/plans", admin, null));
        for (JsonNode plan : plans) {
            if ("FREE".equals(plan.get("code").asText())) {
                assertThat(limit(plan, "PHOTOS_PER_SERIES").get("value").asLong()).isEqualTo(8);
                assertThat(limit(plan, "PHOTOS_PER_SERIES").get("customized").asBoolean()).isTrue();
            }
        }
    }

    @Test
    void shouldRefuseARequestWithoutToken() throws Exception {
        call(HttpMethod.GET, "/api/admin/plans", null, null)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRefuseAnAthleteWhoIsNotAdmin() throws Exception {
        call(HttpMethod.GET, "/api/admin/plans", athlete(), null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private long athletes(String admin, String code) throws Exception {
        for (JsonNode plan : data(call(HttpMethod.GET, "/api/admin/plans", admin, null))) {
            if (code.equals(plan.get("code").asText())) {
                return plan.get("athleteCount").asLong();
            }
        }
        throw new AssertionError("plano não encontrado: " + code);
    }

    static JsonNode limit(JsonNode plan, String key) {
        for (JsonNode limit : plan.get("limits")) {
            if (key.equals(limit.get("key").asText())) {
                return limit;
            }
        }
        throw new AssertionError("limite não encontrado: " + key);
    }
}
