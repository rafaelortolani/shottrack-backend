package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import com.shottrack.backend.support.SeriesTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC66: o atleta consulta o próprio plano, os limites efetivos e o uso. */
class MyPlanTest extends PlanApiTest {

    @Test
    void shouldShowPlanLimitsAndZeroUsageForANewAthlete() throws Exception {
        String token = athlete();

        JsonNode mine = data(call(HttpMethod.GET, "/api/users/me/plan", token, null).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store"))));

        assertThat(mine.get("plan").get("code").asText()).isEqualTo("FREE");
        assertThat(mine.get("plan").get("name").asText()).isEqualTo("Gratuito");
        assertThat(mine.get("limits").get("photosPerSeries").asLong()).isEqualTo(5);
        assertThat(mine.get("limits").get("photosPerAthlete").asLong()).isEqualTo(500);
        assertThat(mine.get("limits").get("storageBytesPerAthlete").asLong()).isEqualTo(536_870_912L);
        assertThat(mine.get("limits").get("uploadsPerHour").asLong()).isEqualTo(30);
        assertThat(mine.get("usage").get("photos").asLong()).isZero();
        assertThat(mine.get("usage").get("storageBytes").asLong()).isZero();
        assertThat(mine.fieldNames()).toIterable().containsExactlyInAnyOrder("plan", "limits", "usage");
    }

    @Test
    void shouldCountPhotosAndSumTheirSize() throws Exception {
        String token = athlete();
        UUID series = PlanEnforcementTest.createSeries(mockMvc, objectMapper, token, SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC"));
        PlanEnforcementTest.upload(mockMvc, token, series).andExpect(status().isCreated());
        PlanEnforcementTest.upload(mockMvc, token, series).andExpect(status().isCreated());
        long expected = jdbc.queryForObject("SELECT sum(size_bytes) FROM series_photos WHERE series_id = ?", Long.class, series);

        JsonNode mine = data(call(HttpMethod.GET, "/api/users/me/plan", token, null));

        assertThat(mine.get("usage").get("photos").asLong()).isEqualTo(2);
        assertThat(mine.get("usage").get("storageBytes").asLong()).isEqualTo(expected);
    }

    @Test
    void shouldReflectPlanAndLimitChangesMadeByTheAdmin() throws Exception {
        String admin = admin();
        createPlan(admin, "PAID", "Pago");
        String email = athleteEmail("reflete");
        String token = athlete(email);
        call(HttpMethod.GET, "/api/users/me/plan", token, null).andExpect(status().isOk());

        call(HttpMethod.PATCH, "/api/admin/plans/PAID/limits", admin, Map.of("limits", Map.of("PHOTOS_PER_SERIES", 15))).andExpect(status().isOk());
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "PAID")).andExpect(status().isOk());

        JsonNode mine = data(call(HttpMethod.GET, "/api/users/me/plan", token, null));
        assertThat(mine.get("plan").get("code").asText()).isEqualTo("PAID");
        assertThat(mine.get("limits").get("photosPerSeries").asLong()).isEqualTo(15);

        call(HttpMethod.PATCH, "/api/admin/plans/PAID/limits", admin, Map.of("limits", Map.of("PHOTOS_PER_SERIES", 3))).andExpect(status().isOk());
        assertThat(data(call(HttpMethod.GET, "/api/users/me/plan", token, null)).get("limits").get("photosPerSeries").asLong()).isEqualTo(3);
    }

    @Test
    void shouldShowEachAthleteOnlyTheirOwnPlanAndUsage() throws Exception {
        String first = athlete();
        UUID series = PlanEnforcementTest.createSeries(mockMvc, objectMapper, first, SeriesTestSupport.openTraining(mockMvc, objectMapper, first, "IPSC"));
        PlanEnforcementTest.upload(mockMvc, first, series).andExpect(status().isCreated());
        String second = athlete();

        assertThat(data(call(HttpMethod.GET, "/api/users/me/plan", first, null)).get("usage").get("photos").asLong()).isEqualTo(1);
        assertThat(data(call(HttpMethod.GET, "/api/users/me/plan", second, null)).get("usage").get("photos").asLong()).isZero();
    }

    @Test
    void shouldRefuseARequestWithoutToken() throws Exception {
        call(HttpMethod.GET, "/api/users/me/plan", null, null)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
