package com.shottrack.backend.application.visit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.series.gateway.repository.SeriesRepository;
import com.shottrack.backend.application.series.gateway.repository.SeriesResultRepository;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.visit.gateway.repository.TrainingRepository;
import com.shottrack.backend.application.visit.gateway.repository.VisitRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitDeleteTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private SeriesRepository seriesRepository;

    @Autowired
    private SeriesResultRepository seriesResultRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldDeleteVisitWithoutTrainings() throws Exception {
        String token = registerAndLogin("atleta.excluivisitasemtreino@shottrack.com");
        UUID visitId = SeriesTestSupport.openVisit(mockMvc, objectMapper, token);
        mockMvc.perform(post("/api/visits/" + visitId + "/closure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(delete("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldDeleteVisitWithTrainingsAndSeriesInCascade() throws Exception {
        String token = registerAndLogin("atleta.excluivisitacomtreino@shottrack.com");
        UUID visitId = SeriesTestSupport.openVisit(mockMvc, objectMapper, token);
        UUID firstTrainingId = SeriesTestSupport.openTrainingInVisit(mockMvc, objectMapper, token, visitId, "IPSC");
        UUID secondTrainingId = SeriesTestSupport.openTrainingInVisit(mockMvc, objectMapper, token, visitId, "Trap");
        UUID firstSeriesId = registerSeries(token, firstTrainingId);
        UUID secondSeriesId = registerSeries(token, secondTrainingId);
        registerResult(token, firstSeriesId, resultTypeIdByName(token, "Tempo"), "10");

        mockMvc.perform(delete("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        flushAndClear();
        assertThat(visitRepository.existsById(visitId)).isFalse();
        assertThat(trainingRepository.existsById(firstTrainingId)).isFalse();
        assertThat(trainingRepository.existsById(secondTrainingId)).isFalse();
        assertThat(seriesRepository.existsById(firstSeriesId)).isFalse();
        assertThat(seriesRepository.existsById(secondSeriesId)).isFalse();
        assertThat(seriesResultRepository.findAllBySeriesId(firstSeriesId)).isEmpty();
    }

    @Test
    void shouldDeleteVisitInProgress() throws Exception {
        String token = registerAndLogin("atleta.excluivisitaemandamento@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var visits = mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[0].trainings[0].status").value("IN_PROGRESS"))
                .andReturn();
        UUID visitId = UUID.fromString(objectMapper.readTree(visits.getResponse().getContentAsString())
                .get("data").get(0).get("id").asText());

        mockMvc.perform(delete("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        flushAndClear();
        assertThat(visitRepository.existsById(visitId)).isFalse();
        assertThat(trainingRepository.existsById(trainingId)).isFalse();
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/visits/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentVisit() throws Exception {
        String token = registerAndLogin("atleta.excluivisitainexistente@shottrack.com");

        mockMvc.perform(delete("/api/visits/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
    }

    @Test
    void shouldRejectDeletingAnotherAthletesVisit() throws Exception {
        String tokenDono = registerAndLogin("atleta.donovisitaexclusao@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonovisitaexclusao@shottrack.com");
        UUID visitId = SeriesTestSupport.openVisit(mockMvc, objectMapper, tokenDono);

        mockMvc.perform(delete("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));

        flushAndClear();
        assertThat(visitRepository.existsById(visitId)).isTrue();
    }

    /**
     * A cascata acontece no banco (ON DELETE CASCADE, V38), invisível pro
     * contexto de persistência do Hibernate: sem flush o DELETE da visita
     * ainda nem foi enviado, e sem clear treinos/séries continuariam em cache.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private UUID registerSeries(String token, UUID trainingId) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private void registerResult(String token, UUID seriesId, UUID resultTypeId, String value) throws Exception {
        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(resultTypeId, value))))
                .andExpect(status().isOk());
    }

    private UUID resultTypeIdByName(String token, String name) throws Exception {
        var result = mockMvc.perform(get("/api/result-type-catalog")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Tipo de resultado não encontrado no catálogo: " + name);
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
