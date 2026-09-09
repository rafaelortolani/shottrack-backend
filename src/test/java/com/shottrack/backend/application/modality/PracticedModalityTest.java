package com.shottrack.backend.application.modality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
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
class PracticedModalityTest {

    private static final String EMAIL = "atleta.praticadas@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private UUID ipscId;
    private UUID trapId;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Atleta Praticadas", EMAIL, PASSWORD))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();

        ipscId = modalityIdByName("IPSC");
        trapId = modalityIdByName("Trap");
    }

    @Test
    void shouldAddModality() throws Exception {
        mockMvc.perform(post("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(ipscId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("IPSC"));
    }

    @Test
    void shouldRemoveModalityWithoutAffectingOthers() throws Exception {
        addModality(ipscId);
        addModality(trapId);

        mockMvc.perform(delete("/api/practiced-modalities/" + ipscId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Trap"));
    }

    @Test
    void shouldListPracticedModalities() throws Exception {
        addModality(ipscId);
        addModality(trapId);

        mockMvc.perform(get("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.name == 'IPSC')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Trap')]").isNotEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenNoPracticedModalities() throws Exception {
        mockMvc.perform(get("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/practiced-modalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(ipscId))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentModality() throws Exception {
        mockMvc.perform(post("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_FOUND"));
    }

    @Test
    void shouldRejectAlreadyAddedModality() throws Exception {
        addModality(ipscId);

        mockMvc.perform(post("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(ipscId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MODALITY_ALREADY_ADDED"));
    }

    @Test
    void shouldRejectRemovingUnassociatedModality() throws Exception {
        mockMvc.perform(delete("/api/practiced-modalities/" + ipscId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODALITY_NOT_ASSOCIATED"));
    }

    private void addModality(UUID modalityId) throws Exception {
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
}
