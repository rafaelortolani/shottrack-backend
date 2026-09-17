package com.shottrack.backend.application.accessory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AccessoryUpdateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessoryUpdateTest {

    private static final String EMAIL = "atleta.editaacessorio@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;
    private UUID accessoryId;

    @BeforeEach
    void prepareExistingAccessory() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Edita Acessório", EMAIL, PASSWORD);

        var registerRequest = new AccessoryRegisterRequest("Original", "Luneta", "Observação original");
        var registerResult = mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();
        accessoryId = UUID.fromString(objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    @Test
    void shouldEditOnlyNameKeepingTheRest() throws Exception {
        var request = new AccessoryUpdateRequest("Nome novo", null, null);

        mockMvc.perform(patch("/api/accessories/" + accessoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nome novo"))
                .andExpect(jsonPath("$.data.type").value("Luneta"))
                .andExpect(jsonPath("$.data.notes").value("Observação original"));
    }

    @Test
    void shouldEditMultipleFieldsAtOnce() throws Exception {
        var request = new AccessoryUpdateRequest("Nome novo", "Bipé", "Observação nova");

        mockMvc.perform(patch("/api/accessories/" + accessoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nome novo"))
                .andExpect(jsonPath("$.data.type").value("Bipé"))
                .andExpect(jsonPath("$.data.notes").value("Observação nova"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        var request = new AccessoryUpdateRequest("Nome novo", null, null);

        mockMvc.perform(patch("/api/accessories/" + accessoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentAccessory() throws Exception {
        var request = new AccessoryUpdateRequest("Nome novo", null, null);

        mockMvc.perform(patch("/api/accessories/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_NOT_FOUND"));
    }

    @Test
    void shouldRejectEmptyName() throws Exception {
        var request = new AccessoryUpdateRequest("   ", null, null);

        mockMvc.perform(patch("/api/accessories/" + accessoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ACCESSORY_NAME_REQUIRED"));
    }
}
