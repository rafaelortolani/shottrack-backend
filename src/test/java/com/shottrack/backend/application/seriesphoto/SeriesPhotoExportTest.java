package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC53: a exportação lista, por série, só os metadados das fotos (sem id, sem chave de armazenamento, sem a imagem). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesPhotoExportTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private UUID series(String token, UUID training) throws Exception {
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(training, null, null, null, null, null, null))))
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private void photo(String token, UUID series, int width, int height) throws Exception {
        mockMvc.perform(multipart("/api/series/" + series + "/photos")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.jpeg(width, height)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private String export(String token) throws Exception {
        return mockMvc.perform(post("/api/users/me/export").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void shouldListThePhotoMetadataPerSeriesAndNothingElse() throws Exception {
        String mine = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Exporta", "foto.exporta@shottrack.com", PASSWORD);
        String other = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Outro", "foto.exporta.outro@shottrack.com", PASSWORD);
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, mine, "IPSC");
        UUID withPhotos = series(mine, training);
        series(mine, training); // série sem foto
        photo(mine, withPhotos, 90, 60);
        photo(mine, withPhotos, 50, 50);
        photo(other, series(other, SeriesTestSupport.openTraining(mockMvc, objectMapper, other, "IPSC")), 30, 30);

        String body = export(mine);

        JsonNode series = objectMapper.readTree(body).get("visits").get(0).get("trainings").get(0).get("series");
        assertThat(series).hasSize(2);
        JsonNode photos = null;
        int empty = 0;
        for (JsonNode s : series) {
            if (s.get("photos").isEmpty()) {
                empty++;
            } else {
                photos = s.get("photos");
            }
        }
        assertThat(empty).as("série sem foto traz lista vazia").isEqualTo(1);
        assertThat(photos).hasSize(2);
        assertThat(photos.get(0).get("widthPx").asInt()).isEqualTo(90);
        assertThat(photos.get(0).get("heightPx").asInt()).isEqualTo(60);
        assertThat(photos.get(0).get("sizeBytes").asLong()).isPositive();
        assertThat(photos.get(0).get("createdAt").asText()).isNotBlank();
        assertThat(photos.get(1).get("widthPx").asInt()).isEqualTo(50);
        assertThat(photos.get(0).fieldNames()).toIterable().containsExactlyInAnyOrder("createdAt", "widthPx", "heightPx", "sizeBytes");
        assertThat(body).doesNotContain("storageKey").doesNotContain("thumbKey").doesNotContain("athletes/").doesNotContain(".jpg");
    }

    @Test
    void shouldNotListPhotosOfAnotherAthlete() throws Exception {
        String mine = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Sem Foto", "foto.exporta.sem@shottrack.com", PASSWORD);
        String other = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Com Foto", "foto.exporta.com@shottrack.com", PASSWORD);
        UUID myTraining = SeriesTestSupport.openTraining(mockMvc, objectMapper, mine, "IPSC");
        series(mine, myTraining);
        photo(other, series(other, SeriesTestSupport.openTraining(mockMvc, objectMapper, other, "IPSC")), 30, 30);

        JsonNode series = objectMapper.readTree(export(mine)).get("visits").get(0).get("trainings").get(0).get("series");

        assertThat(series.get(0).get("photos")).isEmpty();
    }
}
