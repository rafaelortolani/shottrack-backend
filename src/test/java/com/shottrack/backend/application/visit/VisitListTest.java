package com.shottrack.backend.application.visit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.visit.dto.OpenTrainingRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitListTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldListVisitsWithNestedTrainings() throws Exception {
        String token = registerAndLogin("atleta.listavisitas@shottrack.com");
        UUID modalityId = practiceModality(token, "IPSC");
        UUID trainingLocationId = registerTrainingLocation(token);
        UUID visitComTreino = startVisit(token, trainingLocationId);
        startVisit(token, trainingLocationId);
        openTraining(token, visitComTreino, modalityId);

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.id == '" + visitComTreino + "')].trainings[0].modalityName").value("IPSC"))
                .andExpect(jsonPath("$.data[?(@.id == '" + visitComTreino + "')].trainings[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[?(@.id != '" + visitComTreino + "')].trainings[0]").doesNotExist());
    }

    @Test
    void shouldReturnEmptyListWhenNoVisits() throws Exception {
        String token = registerAndLogin("atleta.semvisitas@shottrack.com");

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/visits"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldNeverIncludeAnotherAthletesVisit() throws Exception {
        String tokenDono = registerAndLogin("atleta.donovisitalistagem@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonovisitalistagem@shottrack.com");
        startVisit(tokenDono, registerTrainingLocation(tokenDono));

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private UUID startVisit(String token, UUID trainingLocationId) throws Exception {
        var result = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(trainingLocationId, null))))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private void openTraining(String token, UUID visitId, UUID modalityId) throws Exception {
        mockMvc.perform(post("/api/trainings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OpenTrainingRequest(visitId, modalityId))));
    }

    private UUID practiceModality(String token, String modalityName) throws Exception {
        UUID modalityId = modalityIdByName(token, modalityName);

        mockMvc.perform(post("/api/practiced-modalities")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(modalityId))));

        return modalityId;
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
