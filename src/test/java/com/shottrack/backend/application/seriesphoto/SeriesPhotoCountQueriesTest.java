package com.shottrack.backend.application.seriesphoto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.SqlRecorder;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC37 / ADR-0026: o `photoCount` de cada série vem de UM comando SQL agrupado, seja a lista de 1 ou de 10 séries.
 * Conta os comandos SQL de verdade (inspetor de comandos do Hibernate, ligado só neste teste), não consultas distintas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.shottrack.backend.support.SqlRecorder")
class SeriesPhotoCountQueriesTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SeriesPhotoGateway photoGateway;
    @Autowired
    private EntityManager entityManager;

    private UUID registerSeriesWithPhotos(String token, UUID athleteId, UUID trainingId, int photos) throws Exception {
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        UUID seriesId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
        for (int position = 1; position <= photos; position++) {
            UUID photoId = UUID.randomUUID();
            photoGateway.save(SeriesPhoto.builder().id(photoId).seriesId(seriesId).athleteId(athleteId)
                    .storageKey("athletes/x/series/y/" + photoId + ".jpg").thumbKey("athletes/x/series/y/" + photoId + ".thumb.jpg")
                    .position(position).widthPx(10).heightPx(10).sizeBytes(1).build());
        }
        return seriesId;
    }

    /** Lista as séries do treino e devolve {comandos SQL no total, comandos SQL em series_photos}. */
    private long[] listAndCount(String token, UUID trainingId, int expectedSeries, int expectedPhotosEach) throws Exception {
        entityManager.flush();
        entityManager.clear();
        SqlRecorder.clear();
        var result = mockMvc.perform(get("/api/trainings/" + trainingId + "/series").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        long total = SqlRecorder.statements().size();
        long onPhotos = SqlRecorder.countContaining("series_photos");

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(list).hasSize(expectedSeries);
        list.forEach(series -> assertThat(series.get("photoCount").asInt()).isEqualTo(expectedPhotosEach));
        return new long[]{total, onPhotos};
    }

    @Test
    void shouldUseTheSameNumberOfSqlCommandsOnPhotosWhetherTheTrainingHasOneOrTenSeries() throws Exception {
        String email = "foto.sql." + UUID.randomUUID() + "@shottrack.com";
        String token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta SQL", email, PASSWORD);
        UUID athleteId = userRepository.findByEmail(email).orElseThrow().getId();
        UUID trainingOne = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID trainingTen = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeriesWithPhotos(token, athleteId, trainingOne, 3);
        for (int i = 0; i < 10; i++) {
            registerSeriesWithPhotos(token, athleteId, trainingTen, 3);
        }

        long[] one = listAndCount(token, trainingOne, 1, 3);
        long[] ten = listAndCount(token, trainingTen, 10, 3);

        System.out.println("UC37 comandos SQL — 1 série: total " + one[0] + ", em series_photos " + one[1]
                + " | 10 séries: total " + ten[0] + ", em series_photos " + ten[1]);
        assertThat(one[1]).as("1 série: um único comando em series_photos").isEqualTo(1);
        assertThat(ten[1]).as("10 séries: o MESMO número de comandos em series_photos (sem N+1)").isEqualTo(one[1]);
    }
}
