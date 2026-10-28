package com.shottrack.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Montagem de dados dos testes do dashboard (UC42 v7 e UC46): catálogo por
 * nome, tipo de resultado configurado na modalidade, séries com disparos e
 * resultados.
 */
public final class DashboardTestSupport {

    private DashboardTestSupport() {
    }

    public static UUID modalityId(MockMvc mockMvc, ObjectMapper objectMapper, String token, String name) throws Exception {
        return idByName(mockMvc, objectMapper, token, "/api/modality-catalog", name);
    }

    public static UUID resultTypeId(MockMvc mockMvc, ObjectMapper objectMapper, String token, String name) throws Exception {
        return idByName(mockMvc, objectMapper, token, "/api/result-type-catalog", name);
    }

    /** Abre o treino (que também passa a praticar a modalidade) e configura o tipo nela (UC30). */
    public static void configureResultType(MockMvc mockMvc, ObjectMapper objectMapper, String token, UUID modalityId,
                                           UUID resultTypeId) throws Exception {
        mockMvc.perform(post("/api/practiced-modalities/" + modalityId + "/result-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(resultTypeId))));
    }

    public static UUID registerSeries(MockMvc mockMvc, ObjectMapper objectMapper, String token, UUID trainingId,
                                      Integer shotCount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, shotCount, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    public static void registerSeriesWithResult(MockMvc mockMvc, ObjectMapper objectMapper, String token, UUID trainingId,
                                                UUID resultTypeId, String value) throws Exception {
        UUID seriesId = registerSeries(mockMvc, objectMapper, token, trainingId, null);
        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(resultTypeId, value))))
                .andExpect(status().isOk());
    }

    private static UUID idByName(MockMvc mockMvc, ObjectMapper objectMapper, String token, String path, String name)
            throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token)).andReturn();
        for (JsonNode item : objectMapper.readTree(result.getResponse().getContentAsString()).get("data")) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Item não encontrado no catálogo (" + path + "): " + name);
    }
}
