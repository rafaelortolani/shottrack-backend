package com.shottrack.backend.application.modality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
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
class ModalityResultTypeTest {

    private static final String EMAIL = "atleta.perfilmodalidade@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;
    private UUID ipscId;
    private UUID trapId;
    private UUID pontuacaoId;
    private UUID tempoId;
    private UUID agrupamentoId;
    private UUID fatorDesempenhoId;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Perfil Modalidade", EMAIL, PASSWORD);

        ipscId = modalityIdByName("IPSC");
        trapId = modalityIdByName("Trap");

        pontuacaoId = resultTypeIdByName("Pontuação");
        tempoId = resultTypeIdByName("Tempo");
        agrupamentoId = resultTypeIdByName("Agrupamento");
        fatorDesempenhoId = resultTypeIdByName("Fator de desempenho");

        addPracticedModality(ipscId);
    }

    @Test
    void shouldListConfiguredResultTypesWithDefaultSuggestionApplied() throws Exception {
        mockMvc.perform(get("/api/practiced-modalities/" + ipscId + "/result-types")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.name == 'Tempo')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Pontuação')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Fator de desempenho')]").isNotEmpty());
    }

    @Test
    void shouldAddResultType() throws Exception {
        mockMvc.perform(post("/api/practiced-modalities/" + ipscId + "/result-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(agrupamentoId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Agrupamento"));

        mockMvc.perform(get("/api/practiced-modalities/" + ipscId + "/result-types")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[?(@.name == 'Agrupamento')]").isNotEmpty());
    }

    @Test
    void shouldRemoveResultTypeWithoutAffectingOthers() throws Exception {
        mockMvc.perform(delete("/api/practiced-modalities/" + ipscId + "/result-types/" + pontuacaoId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/practiced-modalities/" + ipscId + "/result-types")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.name == 'Pontuação')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Tempo')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Fator de desempenho')]").isNotEmpty());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/practiced-modalities/" + ipscId + "/result-types"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectWhenModalityNotPracticed() throws Exception {
        mockMvc.perform(get("/api/practiced-modalities/" + trapId + "/result-types")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_PRACTICED"));
    }

    @Test
    void shouldRejectResultTypeNotFound() throws Exception {
        mockMvc.perform(post("/api/practiced-modalities/" + ipscId + "/result-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESULT_TYPE_NOT_FOUND"));
    }

    @Test
    void shouldRejectAlreadyConfiguredResultType() throws Exception {
        mockMvc.perform(post("/api/practiced-modalities/" + ipscId + "/result-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddModalityResultTypeRequest(tempoId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESULT_TYPE_ALREADY_CONFIGURED"));
    }

    @Test
    void shouldRejectRemovingNotConfiguredResultType() throws Exception {
        mockMvc.perform(delete("/api/practiced-modalities/" + ipscId + "/result-types/" + agrupamentoId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESULT_TYPE_NOT_CONFIGURED"));
    }

    private void addPracticedModality(UUID modalityId) throws Exception {
        mockMvc.perform(post("/api/practiced-modalities")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(modalityId))));
    }

    private UUID modalityIdByName(String name) throws Exception {
        var result = mockMvc.perform(get("/api/modality-catalog")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Modalidade não encontrada no catálogo: " + name);
    }

    private UUID resultTypeIdByName(String name) throws Exception {
        var result = mockMvc.perform(get("/api/result-type-catalog")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Tipo de resultado não encontrado no catálogo: " + name);
    }
}
