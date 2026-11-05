package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.support.PlanApiTest;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC60 com limites do plano: lidos do plano do atleta, dinâmicos, sem apagar dado quando reduzidos. */
class PlanEnforcementTest extends PlanApiTest {

    static UUID createSeries(MockMvc mockMvc, ObjectMapper objectMapper, String token, UUID training) throws Exception {
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null)))).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    static ResultActions upload(MockMvc mockMvc, String token, UUID series) throws Exception {
        return upload(mockMvc, token, series, TestImages.jpeg(40, 40));
    }

    static ResultActions upload(MockMvc mockMvc, String token, UUID series, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/series/" + series + "/photos")
                .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", bytes)).header("Authorization", "Bearer " + token));
    }

    private void limits(String admin, Map<String, Object> limits) throws Exception {
        call(HttpMethod.PATCH, "/api/admin/plans/FREE/limits", admin, Map.of("limits", limits)).andExpect(status().isOk());
    }

    private UUID series(String token) throws Exception {
        return createSeries(mockMvc, objectMapper, token, SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC"));
    }

    @Test
    void shouldReadThePerSeriesLimitFromThePlan() throws Exception {
        String admin = admin();
        limits(admin, Map.of("PHOTOS_PER_SERIES", 2));
        String token = athlete();
        UUID series = series(token);

        upload(mockMvc, token, series).andExpect(status().isCreated());
        upload(mockMvc, token, series).andExpect(status().isCreated());
        upload(mockMvc, token, series).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PHOTO_LIMIT_PER_SERIES_REACHED"));
    }

    @Test
    void shouldReadThePerAthleteLimitFromThePlan() throws Exception {
        String admin = admin();
        limits(admin, Map.of("PHOTOS_PER_ATHLETE", 1));
        String token = athlete();

        upload(mockMvc, token, series(token)).andExpect(status().isCreated());
        upload(mockMvc, token, series(token)).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PHOTO_LIMIT_PER_ATHLETE_REACHED"));
    }

    @Test
    void shouldRefuseWhenTheStorageLimitOfThePlanWouldBeExceeded() throws Exception {
        byte[] image = TestImages.jpeg(40, 40);
        String admin = admin();
        String token = athlete();
        UUID series = series(token);
        upload(mockMvc, token, series, image).andExpect(status().isCreated());
        long used = jdbc.queryForObject("SELECT sum(size_bytes) FROM series_photos WHERE athlete_id = (SELECT id FROM users ORDER BY created_at DESC LIMIT 1)", Long.class);
        // espaço para a foto existente + 1 byte: a próxima (maior que 1 byte) não cabe
        limits(admin, Map.of("STORAGE_BYTES_PER_ATHLETE", used + 1));

        upload(mockMvc, token, series, image).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PHOTO_STORAGE_LIMIT_REACHED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_photos WHERE athlete_id = (SELECT id FROM users ORDER BY created_at DESC LIMIT 1)", Long.class)).isEqualTo(1);
    }

    @Test
    void shouldCountTheProcessedImageSizeNotTheUploadedSizeNorTheThumbnail() throws Exception {
        String token = athlete();
        UUID series = series(token);
        upload(mockMvc, token, series).andExpect(status().isCreated());
        var photo = jdbc.queryForMap("SELECT size_bytes, storage_key FROM series_photos ORDER BY created_at DESC LIMIT 1");

        var mine = data(call(HttpMethod.GET, "/api/users/me/plan", token, null));
        assertThat(mine.get("usage").get("storageBytes").asLong()).isEqualTo(((Number) photo.get("size_bytes")).longValue());
    }

    @Test
    void shouldAcceptAPhotoExactlyAtTheStorageLimit() throws Exception {
        byte[] image = TestImages.jpeg(40, 40);
        String admin = admin();
        String token = athlete();
        UUID series = series(token);
        upload(mockMvc, token, series, image).andExpect(status().isCreated());
        long size = jdbc.queryForObject("SELECT size_bytes FROM series_photos ORDER BY created_at DESC LIMIT 1", Long.class);
        limits(admin, Map.of("STORAGE_BYTES_PER_ATHLETE", size * 2));

        upload(mockMvc, token, series, image).andExpect(status().isCreated());
        upload(mockMvc, token, series, image).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PHOTO_STORAGE_LIMIT_REACHED"));
    }

    @Test
    void shouldKeepExistingPhotosAndRefuseNewOnesWhenTheLimitIsReduced_thenReleaseWhenRaised() throws Exception {
        String admin = admin();
        String token = athlete();
        UUID series = series(token);
        upload(mockMvc, token, series).andExpect(status().isCreated());
        upload(mockMvc, token, series).andExpect(status().isCreated());
        upload(mockMvc, token, series).andExpect(status().isCreated());

        limits(admin, Map.of("PHOTOS_PER_SERIES", 1));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_photos WHERE series_id = ?", Long.class, series)).as("nada apagado").isEqualTo(3);
        upload(mockMvc, token, series).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PHOTO_LIMIT_PER_SERIES_REACHED"));

        // aumentar libera o envio seguinte (cache invalidado)
        limits(admin, Map.of("PHOTOS_PER_SERIES", 10));
        upload(mockMvc, token, series).andExpect(status().isCreated());
    }

    @Test
    void shouldStillLetTheAthleteDeletePhotosWhileOverTheLimit() throws Exception {
        String admin = admin();
        String token = athlete();
        UUID series = series(token);
        upload(mockMvc, token, series).andExpect(status().isCreated());
        upload(mockMvc, token, series).andExpect(status().isCreated());
        limits(admin, Map.of("PHOTOS_PER_SERIES", 0, "PHOTOS_PER_ATHLETE", 0));
        UUID photoId = jdbc.queryForObject("SELECT id FROM series_photos WHERE series_id = ? ORDER BY position LIMIT 1", UUID.class, series);

        mockMvc.perform(delete("/api/series/" + series + "/photos/" + photoId).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_photos WHERE series_id = ?", Long.class, series)).isEqualTo(1);
    }

    @Test
    void shouldApplyThePlanChangeOfTheAthleteImmediately() throws Exception {
        String admin = admin();
        createPlan(admin, "TINY", "Minúsculo");
        call(HttpMethod.PATCH, "/api/admin/plans/TINY/limits", admin, Map.of("limits", Map.of("PHOTOS_PER_SERIES", 1))).andExpect(status().isOk());
        String email = athleteEmail("muda.plano");
        String token = athlete(email);
        UUID series = series(token);
        upload(mockMvc, token, series).andExpect(status().isCreated());
        upload(mockMvc, token, series).andExpect(status().isCreated());

        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "TINY")).andExpect(status().isOk());

        upload(mockMvc, token, series).andExpect(status().isConflict());
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "FREE")).andExpect(status().isOk());
        upload(mockMvc, token, series).andExpect(status().isCreated());
    }

    @Test
    void shouldReadTheHourlyUploadLimitFromThePlanAndFollowChangesWithoutLosingTheConsumption() throws Exception {
        String admin = admin();
        limits(admin, Map.of("UPLOADS_PER_HOUR", 2));
        String token = athlete();
        UUID series = series(token);

        upload(mockMvc, token, series, "texto".getBytes()).andExpect(status().isUnsupportedMediaType());
        upload(mockMvc, token, series, "texto".getBytes()).andExpect(status().isUnsupportedMediaType());
        upload(mockMvc, token, series).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));

        // o administrador aumenta o limite: o atleta volta a enviar sem esperar a hora
        limits(admin, Map.of("UPLOADS_PER_HOUR", 10));
        upload(mockMvc, token, series).andExpect(status().isCreated());
    }

    @Test
    void shouldGiveEachAthleteTheirOwnHourlyLimit() throws Exception {
        String admin = admin();
        createPlan(admin, "BUSY", "Movimentado");
        call(HttpMethod.PATCH, "/api/admin/plans/BUSY/limits", admin, Map.of("limits", Map.of("UPLOADS_PER_HOUR", 1))).andExpect(status().isOk());
        String email = athleteEmail("limite.proprio");
        String limited = athlete(email);
        call(HttpMethod.PUT, "/api/admin/users/" + userId(email) + "/plan", admin, Map.of("planCode", "BUSY")).andExpect(status().isOk());
        String normal = athlete();

        UUID limitedSeries = series(limited);
        upload(mockMvc, limited, limitedSeries).andExpect(status().isCreated());
        upload(mockMvc, limited, limitedSeries).andExpect(status().isTooManyRequests());
        UUID normalSeries = series(normal);
        upload(mockMvc, normal, normalSeries).andExpect(status().isCreated());
        upload(mockMvc, normal, normalSeries).andExpect(status().isCreated());
    }
}
