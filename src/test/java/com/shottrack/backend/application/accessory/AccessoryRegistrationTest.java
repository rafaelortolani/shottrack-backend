package com.shottrack.backend.application.accessory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessoryRegistrationTest {

    private static final String EMAIL = "atleta.cadastraacessorio@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;
    private UUID lunetaTypeId;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Cadastra Acessório", EMAIL, PASSWORD);

        lunetaTypeId = idByName("/api/accessory-catalog/types", "Luneta");
    }

    @Test
    void shouldRegisterAccessoryWithValidData() throws Exception {
        var request = new AccessoryRegisterRequest("Luneta 4x32", lunetaTypeId, "Zerada a 100m");

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Luneta 4x32"))
                .andExpect(jsonPath("$.data.type.name").value("Luneta"))
                .andExpect(jsonPath("$.data.notes").value("Zerada a 100m"));
    }

    @Test
    void shouldRegisterAccessoryWithOnlyRequiredFields() throws Exception {
        var request = new AccessoryRegisterRequest("Bipé", lunetaTypeId, null);

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Bipé"))
                .andExpect(jsonPath("$.data.type.name").value("Luneta"))
                .andExpect(jsonPath("$.data.notes").isEmpty());
    }

    @Test
    void shouldRejectEmptyName() throws Exception {
        var request = new AccessoryRegisterRequest("", lunetaTypeId, null);

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMissingTypeId() throws Exception {
        var request = new AccessoryRegisterRequest("Luneta 4x32", null, null);

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        var request = new AccessoryRegisterRequest("Luneta 4x32", lunetaTypeId, null);

        mockMvc.perform(post("/api/accessories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentTypeId() throws Exception {
        var request = new AccessoryRegisterRequest("Luneta 4x32", UUID.randomUUID(), null);

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_TYPE_NOT_FOUND"));
    }

    private UUID idByName(String path, String name) throws Exception {
        var result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Item não encontrado no catálogo (" + path + "): " + name);
    }
}
