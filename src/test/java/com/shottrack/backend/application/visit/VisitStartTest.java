package com.shottrack.backend.application.visit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitStartTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldStartVisit() throws Exception {
        String token = registerAndLogin("atleta.iniciavisita@shottrack.com");
        UUID trainingLocationId = registerTrainingLocation(token);

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(trainingLocationId, "Treino de sábado"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.trainingLocationId").value(trainingLocationId.toString()))
                .andExpect(jsonPath("$.data.observations").value("Treino de sábado"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.endedAt").isEmpty())
                .andExpect(jsonPath("$.data.trainings").isArray())
                .andExpect(jsonPath("$.data.trainings.length()").value(0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(UUID.randomUUID(), null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectMissingTrainingLocationId() throws Exception {
        String token = registerAndLogin("atleta.visitalocalobrigatorio@shottrack.com");

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectNonExistentTrainingLocation() throws Exception {
        String token = registerAndLogin("atleta.visitalocalinexistente@shottrack.com");

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(UUID.randomUUID(), null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_NOT_FOUND"));
    }

    @Test
    void shouldRejectTrainingLocationFromAnotherAthlete() throws Exception {
        String tokenDono = registerAndLogin("atleta.donolocalvisita@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonolocalvisita@shottrack.com");
        UUID trainingLocationId = registerTrainingLocation(tokenDono);

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(trainingLocationId, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_NOT_FOUND"));
    }

    private UUID registerTrainingLocation(String token) throws Exception {
        var request = new TrainingLocationRegisterRequest("Clube de Tiro Central", "São Paulo", "SP");
        var result = mockMvc.perform(post("/api/training-locations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
