package com.shottrack.backend.application.traininglocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationUpdateRequest;
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
class TrainingLocationUpdateTest {

    private static final String EMAIL = "atleta.editalocal@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;
    private UUID trainingLocationId;

    @BeforeEach
    void prepareExistingTrainingLocation() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Edita Local", EMAIL, PASSWORD);

        var registerRequest = new TrainingLocationRegisterRequest("Nome original", "Cidade original", "SP");
        var registerResult = mockMvc.perform(post("/api/training-locations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();
        trainingLocationId = UUID.fromString(objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    @Test
    void shouldEditOnlyNameKeepingTheRest() throws Exception {
        var request = new TrainingLocationUpdateRequest("Nome novo", null, null);

        mockMvc.perform(patch("/api/training-locations/" + trainingLocationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nome novo"))
                .andExpect(jsonPath("$.data.city").value("Cidade original"))
                .andExpect(jsonPath("$.data.state").value("SP"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        var request = new TrainingLocationUpdateRequest("Nome novo", null, null);

        mockMvc.perform(patch("/api/training-locations/" + trainingLocationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentTrainingLocation() throws Exception {
        var request = new TrainingLocationUpdateRequest("Nome novo", null, null);

        mockMvc.perform(patch("/api/training-locations/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_NOT_FOUND"));
    }

    @Test
    void shouldRejectFieldThatWouldBecomeBlank() throws Exception {
        var request = new TrainingLocationUpdateRequest("   ", null, null);

        mockMvc.perform(patch("/api/training-locations/" + trainingLocationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_FIELD_REQUIRED"));
    }
}
