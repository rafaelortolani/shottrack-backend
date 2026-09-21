package com.shottrack.backend.application.series;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.UpdateSeriesRequest;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeriesUpdateTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldEditSingleFieldCompletingMissingData() throws Exception {
        String token = registerAndLogin("atleta.editaserie@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID weaponId = SeriesTestSupport.registerWeapon(mockMvc, objectMapper, token);
        UUID seriesId = registerSeries(token, trainingId);

        mockMvc.perform(patch("/api/series/" + seriesId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSeriesRequest(weaponId, null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weaponId").value(weaponId.toString()))
                .andExpect(jsonPath("$.data.ammunitionId").isEmpty())
                .andExpect(jsonPath("$.data.notes").isEmpty());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/series/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSeriesRequest(null, null, null, null, null, null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectNonExistentSeries() throws Exception {
        String token = registerAndLogin("atleta.serieinexistente@shottrack.com");

        mockMvc.perform(patch("/api/series/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSeriesRequest(null, null, null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
    }

    @Test
    void shouldRejectEditingAnotherAthletesSeries() throws Exception {
        String tokenDono = registerAndLogin("atleta.donoserie@shottrack.com");
        String tokenOutro = registerAndLogin("atleta.naoedonoserie@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, tokenDono, "IPSC");
        UUID seriesId = registerSeries(tokenDono, trainingId);

        mockMvc.perform(patch("/api/series/" + seriesId)
                        .header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSeriesRequest(null, null, null, "Tentativa alheia", null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERIES_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentWeapon() throws Exception {
        String token = registerAndLogin("atleta.editaserie.armainexistente@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);

        mockMvc.perform(patch("/api/series/" + seriesId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSeriesRequest(UUID.randomUUID(), null, null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WEAPON_NOT_FOUND"));
    }

    @Test
    void shouldRejectNonExistentAmmunition() throws Exception {
        String token = registerAndLogin("atleta.editaserie.municaoinexistente@shottrack.com");
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);

        mockMvc.perform(patch("/api/series/" + seriesId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSeriesRequest(null, UUID.randomUUID(), null, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AMMUNITION_NOT_FOUND"));
    }

    private UUID registerSeries(String token, UUID trainingId) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
