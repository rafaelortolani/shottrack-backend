package com.shottrack.backend.application.traininglocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
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

/**
 * UC27/ADR-0006: exclusão bloqueada (nunca arquivamento) se o local já foi
 * usado em alguma visita. O fluxo 2a (TRAINING_LOCATION_IN_USE) não é
 * testável ainda — o domínio de Visita não existe, então nenhum local pode
 * chegar a esse estado (mesma situação de UC08/UC16/UC21). A checagem já
 * está estruturada (TrainingLocationService.isUsedInAnyVisit) pra virar
 * teste real assim que Visita existir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrainingLocationDeleteTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldDeleteTrainingLocationSuccessfully() throws Exception {
        String token = registerAndLogin("atleta.excluilocal@shottrack.com");
        UUID trainingLocationId = registerTrainingLocation(token);

        mockMvc.perform(delete("/api/training-locations/" + trainingLocationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/training-locations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/training-locations/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentTrainingLocation() throws Exception {
        String token = registerAndLogin("atleta.localinexistente@shottrack.com");

        mockMvc.perform(delete("/api/training-locations/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_NOT_FOUND"));
    }

    @Test
    void shouldRejectDeletionOfAnotherAthletesTrainingLocation() throws Exception {
        String tokenDono = registerAndLogin("atleta.donolocal@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonolocal@shottrack.com");
        UUID trainingLocationId = registerTrainingLocation(tokenDono);

        mockMvc.perform(delete("/api/training-locations/" + trainingLocationId)
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_NOT_FOUND"));
    }

    private UUID registerTrainingLocation(String token) throws Exception {
        var request = new TrainingLocationRegisterRequest("Local pra excluir", "São Paulo", "SP");
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
