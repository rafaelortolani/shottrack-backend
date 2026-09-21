package com.shottrack.backend.application.series;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesRegisterTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldRegisterSeriesWithoutOptionalFields() throws Exception {
        String token = registerAndLogin("atleta.serieminima@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.trainingId").value(trainingId.toString()))
                .andExpect(jsonPath("$.data.weaponId").isEmpty())
                .andExpect(jsonPath("$.data.ammunitionId").isEmpty())
                .andExpect(jsonPath("$.data.distanceMeters").isEmpty())
                .andExpect(jsonPath("$.data.target").isEmpty())
                .andExpect(jsonPath("$.data.shotCount").isEmpty())
                .andExpect(jsonPath("$.data.notes").isEmpty())
                .andExpect(jsonPath("$.data.results").isArray())
                .andExpect(jsonPath("$.data.results.length()").value(0));
    }

    @Test
    void shouldRegisterSeriesWithAllFields() throws Exception {
        String token = registerAndLogin("atleta.seriecompleta@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID weaponId = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID ammunitionId = SeriesTestSupport.registerAmmunition(mockMvc, objectMapper, token);

        var request = new RegisterSeriesRequest(trainingId, weaponId, ammunitionId,
                new BigDecimal("25.5"), "Alvo IPSC padrão", 10, "Série de treino de velocidade");

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weaponId").value(weaponId.toString()))
                .andExpect(jsonPath("$.data.ammunitionId").value(ammunitionId.toString()))
                .andExpect(jsonPath("$.data.distanceMeters").value(25.5))
                .andExpect(jsonPath("$.data.target").value("Alvo IPSC padrão"))
                .andExpect(jsonPath("$.data.shotCount").value(10))
                .andExpect(jsonPath("$.data.notes").value("Série de treino de velocidade"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(UUID.randomUUID(), null, null, null, null, null, null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentTraining() throws Exception {
        String token = registerAndLogin("atleta.serietreinoinexistente@shottrack.com");

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(UUID.randomUUID(), null, null, null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_NOT_FOUND"));
    }

    @Test
    void shouldRejectTrainingFromAnotherAthlete() throws Exception {
        String tokenDono = registerAndLogin("atleta.donotreinoserie@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonotreinoserie@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenDono, "IPSC");

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_NOT_FOUND"));
    }

    @Test
    void shouldRejectWhenTrainingAlreadyClosed() throws Exception {
        String token = registerAndLogin("atleta.serietreinoencerrado@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        mockMvc.perform(post("/api/trainings/" + trainingId + "/closure")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TRAINING_ALREADY_CLOSED"));
    }

    @Test
    void shouldRejectNonExistentWeapon() throws Exception {
        String token = registerAndLogin("atleta.seriearmainexistente@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, UUID.randomUUID(), null, null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentAmmunition() throws Exception {
        String token = registerAndLogin("atleta.seriemunicaoinexistente@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, UUID.randomUUID(), null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_NOT_FOUND"));
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
