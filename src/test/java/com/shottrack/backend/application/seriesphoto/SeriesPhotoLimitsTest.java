package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.PlanTestSupport;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC60: limite de fotos por atleta (aqui reduzido a 2 no plano) e limite de envios por hora do plano (aqui 4). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesPhotoLimitsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reducePlanLimits() throws Exception {
        String admin = PlanTestSupport.adminToken(mockMvc, objectMapper, pendingRegistrationRepository, jdbc);
        PlanTestSupport.setLimits(mockMvc, objectMapper, admin, "FREE", Map.of("PHOTOS_PER_ATHLETE", 2, "UPLOADS_PER_HOUR", 4));
    }

    private String athlete(String label) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, label,
                "foto.limite." + UUID.randomUUID() + "@shottrack.com", "senha12345");
    }

    private UUID series(String token) throws Exception {
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private ResultActions upload(String token, UUID series, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/series/" + series + "/photos")
                .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", bytes)).header("Authorization", "Bearer " + token));
    }

    @Test
    void shouldRefuseAPhotoBeyondTheAthleteLimitEvenInAnotherSeries() throws Exception {
        String token = athlete("Atleta Limite");
        UUID first = series(token);
        UUID second = series(token);

        upload(token, first, TestImages.jpeg(40, 40)).andExpect(status().isCreated());
        upload(token, second, TestImages.jpeg(40, 40)).andExpect(status().isCreated());
        upload(token, first, TestImages.jpeg(40, 40))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PHOTO_LIMIT_PER_ATHLETE_REACHED"));
    }

    @Test
    void shouldCountThePhotoLimitPerAthleteNotAcrossAthletes() throws Exception {
        String full = athlete("Atleta Cheio");
        UUID fullSeries = series(full);
        upload(full, fullSeries, TestImages.jpeg(40, 40)).andExpect(status().isCreated());
        upload(full, fullSeries, TestImages.jpeg(40, 40)).andExpect(status().isCreated());

        String other = athlete("Outro Atleta");
        upload(other, series(other), TestImages.jpeg(40, 40)).andExpect(status().isCreated());
    }

    @Test
    void shouldRateLimitUploadsPerAthleteWithRetryAfter() throws Exception {
        String busy = athlete("Atleta Ocupado");
        UUID busySeries = series(busy);
        for (int i = 0; i < 4; i++) {
            // tentativas inválidas também contam: o limite protege o processamento, não só os sucessos
            upload(busy, busySeries, "texto".getBytes()).andExpect(status().isUnsupportedMediaType());
        }

        upload(busy, busySeries, TestImages.jpeg(40, 40))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));

        // a chave é o usuário: outro atleta (mesmo IP) não é afetado
        String calm = athlete("Atleta Calmo");
        upload(calm, series(calm), TestImages.jpeg(40, 40)).andExpect(status().isCreated());
    }
}
