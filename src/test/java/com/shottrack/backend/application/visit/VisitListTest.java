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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC35/ADR-0012: cada visita é listada com seus treinos aninhados. O domínio
 * de Treino (UC32/UC33) ainda não existe, então essa lista aninhada é sempre
 * vazia por enquanto (ver VisitService.trainingsFor) — os testes aqui
 * cobrem a forma da resposta, não conteúdo real de treino.
 */
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
        UUID trainingLocationId = registerTrainingLocation(token);
        startVisit(token, trainingLocationId);
        startVisit(token, trainingLocationId);

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].trainings").isArray())
                .andExpect(jsonPath("$.data[0].trainings.length()").value(0))
                .andExpect(jsonPath("$.data[1].trainings").isArray());
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

    private void startVisit(String token, UUID trainingLocationId) throws Exception {
        mockMvc.perform(post("/api/visits")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartVisitRequest(trainingLocationId, null))));
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
