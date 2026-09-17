package com.shottrack.backend.application.accessory;

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

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Cadastra Acessório", EMAIL, PASSWORD);
    }

    @Test
    void shouldRegisterAccessoryWithValidData() throws Exception {
        var request = new AccessoryRegisterRequest("Luneta 4x32", "Luneta", "Zerada a 100m");

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Luneta 4x32"))
                .andExpect(jsonPath("$.data.type").value("Luneta"))
                .andExpect(jsonPath("$.data.notes").value("Zerada a 100m"));
    }

    @Test
    void shouldRegisterAccessoryWithOnlyRequiredField() throws Exception {
        var request = new AccessoryRegisterRequest("Bipé", null, null);

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Bipé"))
                .andExpect(jsonPath("$.data.type").isEmpty())
                .andExpect(jsonPath("$.data.notes").isEmpty());
    }

    @Test
    void shouldRejectEmptyName() throws Exception {
        var request = new AccessoryRegisterRequest("", null, null);

        mockMvc.perform(post("/api/accessories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        var request = new AccessoryRegisterRequest("Luneta 4x32", "Luneta", null);

        mockMvc.perform(post("/api/accessories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
