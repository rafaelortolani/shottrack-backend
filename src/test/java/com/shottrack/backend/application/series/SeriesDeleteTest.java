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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC41/ADR-0013: "os resultados somem junto" não é testável de ponta a ponta
 * ainda — resultado de série (UC39/UC40) não existe, então nenhuma série
 * pode ter um resultado registrado pra confirmar que ele some (mesma
 * situação de UC08/UC27/UC34 com pendências que dependem de um domínio
 * futuro). A exclusão em si (e o efeito esperado de nada sobrar) já está
 * coberta pelo teste principal abaixo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesDeleteTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldDeleteSeries() throws Exception {
        String token = registerAndLogin("atleta.excluiserie@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);

        mockMvc.perform(delete("/api/series/" + seriesId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/series/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentSeries() throws Exception {
        String token = registerAndLogin("atleta.excluiserieinexistente@shottrack.com");

        mockMvc.perform(delete("/api/series/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
    }

    @Test
    void shouldRejectDeletingAnotherAthletesSeries() throws Exception {
        String tokenDono = registerAndLogin("atleta.donoserieexclusao@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonoserieexclusao@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenDono, "IPSC");
        UUID seriesId = registerSeries(tokenDono, trainingId);

        mockMvc.perform(delete("/api/series/" + seriesId)
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
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

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
