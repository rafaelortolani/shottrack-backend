package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.dashboard.gateway.repository.DashboardSummaryRepository;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC42/ADR-0015: GET /api/dashboard. A lógica de recálculo em si (destaque
 * dinâmico, ignorar valor não numérico etc.) é coberta em
 * DashboardRecalculationServiceTest — aqui o foco é o contrato HTTP e os
 * dois caminhos de leitura (resumo pré-calculado vs. fallback).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private DashboardSummaryRepository dashboardSummaryRepository;

    @Test
    void shouldReadPrecalculatedSummaryWhenItAlreadyExists() throws Exception {
        String token = registerAndLogin("atleta.dashboardresumoexistente@shottrack.com");
        UUID userId = currentUserId(token);

        // valores propositalmente diferentes de qualquer coisa calculável a
        // partir dos dados reais do atleta (não há nenhum) — prova que a
        // leitura vem do resumo, não de um recálculo na hora
        DashboardSummary summary = DashboardSummary.builder().userId(userId).build();
        summary.replaceWith(7, 123, java.util.List.of("Trap", "Skeet"), "Agrupamento", new BigDecimal("3.30"));
        dashboardSummaryRepository.save(summary);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth").value(7))
                .andExpect(jsonPath("$.data.shotsThisMonth").value(123))
                .andExpect(jsonPath("$.data.practicedModalities", org.hamcrest.Matchers.containsInAnyOrder("Trap", "Skeet")))
                .andExpect(jsonPath("$.data.highlight.resultTypeName").value("Agrupamento"))
                .andExpect(jsonPath("$.data.highlight.value").value(3.30))
                .andExpect(jsonPath("$.data.recentVisits.length()").value(0));
    }

    @Test
    void shouldFallBackToOnTheFlyCalculationWhenNoSummaryExistsYet() throws Exception {
        String token = registerAndLogin("atleta.dashboardfallback@shottrack.com");
        UUID userId = currentUserId(token);

        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID modalityId = modalityIdByName(token, "IPSC");
        UUID tempoId = resultTypeIdByName(token, "Tempo");
        mockMvc.perform(post("/api/practiced-modalities/" + modalityId + "/result-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(tempoId))));

        UUID seriesId = registerSeriesWithShotCount(token, trainingId, 25);
        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(tempoId, "9.8"))));

        assertThat(dashboardSummaryRepository.findByUserId(userId)).isEmpty();

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth").value(1))
                .andExpect(jsonPath("$.data.shotsThisMonth").value(25))
                .andExpect(jsonPath("$.data.practicedModalities", org.hamcrest.Matchers.contains("IPSC")))
                .andExpect(jsonPath("$.data.highlight.resultTypeName").value("Tempo"))
                .andExpect(jsonPath("$.data.highlight.value").value(9.8));

        // fallback só calcula, nunca grava
        assertThat(dashboardSummaryRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void shouldReturnZerosAndEmptyListsWhenNoDataYet() throws Exception {
        String token = registerAndLogin("atleta.dashboardsemdados@shottrack.com");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainingsThisMonth").value(0))
                .andExpect(jsonPath("$.data.shotsThisMonth").value(0))
                .andExpect(jsonPath("$.data.practicedModalities.length()").value(0))
                .andExpect(jsonPath("$.data.recentVisits.length()").value(0))
                .andExpect(jsonPath("$.data.highlight").isEmpty());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private UUID registerSeriesWithShotCount(String token, UUID trainingId, int shotCount) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, shotCount, null))))
                .andReturn();
        return idFromResponse(result);
    }

    private UUID currentUserId(String token) throws Exception {
        var result = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andReturn();
        return idFromResponse(result);
    }

    private UUID modalityIdByName(String token, String name) throws Exception {
        return idByName(token, "/api/modality-catalog", name);
    }

    private UUID resultTypeIdByName(String token, String name) throws Exception {
        return idByName(token, "/api/result-type-catalog", name);
    }

    private UUID idByName(String token, String path, String name) throws Exception {
        var result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token)).andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Item não encontrado no catálogo (" + path + "): " + name);
    }

    private UUID idFromResponse(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
