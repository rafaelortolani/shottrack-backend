package com.shottrack.backend.application.series;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.series.dto.MarkSeriesResultNotApplicableRequest;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesResultRemoveTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldRemoveRegisteredValue() throws Exception {
        String token = registerAndLogin("atleta.removevalor@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);
        UUID tempoId = resultTypeIdByName(token, "Tempo");
        registerValue(token, seriesId, tempoId, "10");

        mockMvc.perform(delete("/api/series/" + seriesId + "/results/" + tempoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].results.length()").value(0));
    }

    @Test
    void shouldRemoveNotApplicable() throws Exception {
        String token = registerAndLogin("atleta.removenaoaplicavel@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        mockMvc.perform(post("/api/series/" + seriesId + "/results/not-applicable")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MarkSeriesResultNotApplicableRequest(tempoId))));

        mockMvc.perform(delete("/api/series/" + seriesId + "/results/" + tempoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].results.length()").value(0));
    }

    @Test
    void shouldRecalculateAutoFilledShotCountWhenRemovingHitOrMiss() throws Exception {
        String token = registerAndLogin("atleta.recalcremocao@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID modalityId = modalityIdByName(token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);
        UUID acertosId = configureResultType(token, modalityId, "Acertos");
        UUID errosId = configureResultType(token, modalityId, "Erros");

        registerValue(token, seriesId, acertosId, "45");
        registerValue(token, seriesId, errosId, "10");

        mockMvc.perform(delete("/api/series/" + seriesId + "/results/" + errosId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trainings/" + trainingId + "/series")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].shotCount").value(45));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/series/" + UUID.randomUUID() + "/results/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentSeries() throws Exception {
        String token = registerAndLogin("atleta.removeserieinexistente@shottrack.com");

        mockMvc.perform(delete("/api/series/" + UUID.randomUUID() + "/results/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
    }

    @Test
    void shouldRejectRemovingFromAnotherAthletesSeries() throws Exception {
        String tokenDono = registerAndLogin("atleta.donoserieremocao@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonoserieremocao@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenDono, "IPSC");
        UUID seriesId = registerSeries(tokenDono, trainingId);
        UUID tempoId = resultTypeIdByName(tokenDono, "Tempo");
        registerValue(tokenDono, seriesId, tempoId, "10");

        mockMvc.perform(delete("/api/series/" + seriesId + "/results/" + tempoId)
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
    }

    @Test
    void shouldRejectWhenNoRecordExistsForType() throws Exception {
        String token = registerAndLogin("atleta.semregistro@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        mockMvc.perform(delete("/api/series/" + seriesId + "/results/" + tempoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESULT_NOT_CONFIGURED"));
    }

    private void registerValue(String token, UUID seriesId, UUID resultTypeId, String value) throws Exception {
        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(resultTypeId, value))));
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

    private UUID modalityIdByName(String token, String name) throws Exception {
        var result = mockMvc.perform(get("/api/modality-catalog")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Modalidade não encontrada no catálogo: " + name);
    }

    private UUID configureResultType(String token, UUID modalityId, String resultTypeName) throws Exception {
        UUID resultTypeId = resultTypeIdByName(token, resultTypeName);

        mockMvc.perform(post("/api/practiced-modalities/" + modalityId + "/result-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(resultTypeId))));

        return resultTypeId;
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
