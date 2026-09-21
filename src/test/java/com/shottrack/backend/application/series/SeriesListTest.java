package com.shottrack.backend.application.series;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC37/ADR-0013: cada série é listada com seus resultados. Resultado de
 * série (UC39/UC40) ainda não existe, então essa lista aninhada é sempre
 * vazia por enquanto (ver SeriesService.resultsFor) — os testes aqui
 * cobrem a forma da resposta, não conteúdo real de resultado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesListTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldListSeriesWithResults() throws Exception {
        String token = registerAndLogin("atleta.listaseries@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        registerSeries(token, trainingId);
        registerSeries(token, trainingId);

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].results").isArray())
                .andExpect(jsonPath("$.data[1].results").isArray());
    }

    @Test
    void shouldReturnEmptyListWhenTrainingHasNoSeries() throws Exception {
        String token = registerAndLogin("atleta.semseries@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/trainings/" + UUID.randomUUID() + "/series"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentTraining() throws Exception {
        String token = registerAndLogin("atleta.listaseriestreinoinexistente@shottrack.com");

        mockMvc.perform(get("/api/trainings/" + UUID.randomUUID() + "/series")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_NOT_FOUND"));
    }

    @Test
    void shouldRejectTrainingFromAnotherAthlete() throws Exception {
        String tokenDono = registerAndLogin("atleta.donotreinolistaserie@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonotreinolistaserie@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenDono, "IPSC");

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series")
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_NOT_FOUND"));
    }

    private void registerSeries(String token, UUID trainingId) throws Exception {
        mockMvc.perform(post("/api/series")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))));
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
