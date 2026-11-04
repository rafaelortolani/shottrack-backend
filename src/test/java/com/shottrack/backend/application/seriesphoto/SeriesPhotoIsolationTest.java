package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0026 (Lote 5): o atleta B não lista, vê, baixa miniatura, anexa nem remove foto do atleta A. O esperado é a
 * MESMA resposta de uma série/foto que não existe (não vaza que o recurso é de outra pessoa), com a foto de A intacta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesPhotoIsolationTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String tokenA;
    private String tokenB;
    private UUID seriesA;
    private UUID photoA;
    private UUID seriesB;

    @BeforeEach
    void prepareTwoAthletes() throws Exception {
        tokenA = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta A", "foto.iso.a@shottrack.com", PASSWORD);
        tokenB = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta B", "foto.iso.b@shottrack.com", PASSWORD);
        seriesA = registerSeries(tokenA);
        seriesB = registerSeries(tokenB);
        var upload = mockMvc.perform(multipart("/api/series/" + seriesA + "/photos")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.jpeg(80, 60)))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated()).andReturn();
        photoA = UUID.fromString(objectMapper.readTree(upload.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private UUID registerSeries(String token) throws Exception {
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    private void assertAthleteAPhotoIsIntact() throws Exception {
        mockMvc.perform(get("/api/series/" + seriesA + "/photos").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(photoA.toString()));
        mockMvc.perform(get("/api/series/" + seriesA + "/photos/" + photoA + "/content").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void shouldNotLetAthleteBListAthleteAsPhotos() throws Exception {
        String foreign = body(mockMvc.perform(get("/api/series/" + seriesA + "/photos").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND")));
        String unknown = body(mockMvc.perform(get("/api/series/" + UUID.randomUUID() + "/photos").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()));

        assertThat(foreign).isEqualTo(unknown);
        assertAthleteAPhotoIsIntact();
    }

    @Test
    void shouldNotLetAthleteBSeeContentOrThumbnailOfAthleteAsPhoto() throws Exception {
        for (String kind : new String[]{"content", "thumbnail"}) {
            String foreign = body(mockMvc.perform(get("/api/series/" + seriesA + "/photos/" + photoA + "/" + kind)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND")));
            String unknown = body(mockMvc.perform(get("/api/series/" + UUID.randomUUID() + "/photos/" + UUID.randomUUID() + "/" + kind)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound()));
            assertThat(foreign).isEqualTo(unknown);
        }
        assertAthleteAPhotoIsIntact();
    }

    @Test
    void shouldNotLetAthleteBReachAthleteAsPhotoThroughItsOwnSeries() throws Exception {
        // B usa a própria série com o photoId de A: a foto não pertence àquela série
        for (String suffix : new String[]{"/content", "/thumbnail"}) {
            mockMvc.perform(get("/api/series/" + seriesB + "/photos/" + photoA + suffix).header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
        }
        mockMvc.perform(delete("/api/series/" + seriesB + "/photos/" + photoA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
        assertAthleteAPhotoIsIntact();
    }

    @Test
    void shouldNotLetAthleteBAttachAPhotoToAthleteAsSeries() throws Exception {
        mockMvc.perform(multipart("/api/series/" + seriesA + "/photos")
                        .file(new MockMultipartFile("file", "b.jpg", "image/jpeg", TestImages.jpeg(80, 60)))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));

        mockMvc.perform(get("/api/series/" + seriesA + "/photos").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldNotLetAthleteBRemoveAthleteAsPhoto() throws Exception {
        mockMvc.perform(delete("/api/series/" + seriesA + "/photos/" + photoA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));

        assertAthleteAPhotoIsIntact();
    }

    @Test
    void shouldCountPhotosOnlyOfTheAthletesOwnSeries() throws Exception {
        UUID trainingB = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenB, "IPSC");
        mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + tokenB).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(trainingB, null, null, null, null, null, null))));

        mockMvc.perform(get("/api/trainings/" + trainingB + "/series").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].photoCount").value(0));
    }
}
