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

/**
 * UC34/ADR-0012: encerramento em cascata dos treinos abertos da visita não é
 * testável ainda — o domínio de Treino (UC32/UC33) não existe, então nenhuma
 * visita pode ter um treino aberto pra cascatear (mesma situação de
 * UC08/UC27 com pendências que dependem de um domínio futuro). A chamada já
 * está estruturada em VisitService.close() pra virar teste real assim que
 * Treino existir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitCloseTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldCloseVisitWithoutOpenTrainings() throws Exception {
        String token = registerAndLogin("atleta.encerravisita@shottrack.com");
        UUID visitId = startVisit(token, registerTrainingLocation(token));

        mockMvc.perform(post("/api/visits/" + visitId + "/closure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/visits/" + UUID.randomUUID() + "/closure"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentVisit() throws Exception {
        String token = registerAndLogin("atleta.visitainexistente@shottrack.com");

        mockMvc.perform(post("/api/visits/" + UUID.randomUUID() + "/closure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
    }

    @Test
    void shouldRejectClosingAnotherAthletesVisit() throws Exception {
        String tokenDono = registerAndLogin("atleta.donovisita@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonovisita@shottrack.com");
        UUID visitId = startVisit(tokenDono, registerTrainingLocation(tokenDono));

        mockMvc.perform(post("/api/visits/" + visitId + "/closure")
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
    }

    @Test
    void shouldRejectClosingAlreadyClosedVisit() throws Exception {
        String token = registerAndLogin("atleta.visitajaencerrada@shottrack.com");
        UUID visitId = startVisit(token, registerTrainingLocation(token));

        mockMvc.perform(post("/api/visits/" + visitId + "/closure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/visits/" + visitId + "/closure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VISIT_ALREADY_CLOSED"));
    }

    private UUID startVisit(String token, UUID trainingLocationId) throws Exception {
        var result = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(trainingLocationId, null))))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
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
