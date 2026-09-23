package com.shottrack.backend.application.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.series.dto.UpdateSeriesRequest;
import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.visit.dto.OpenTrainingRequest;
import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * ADR-0015: garante que os 9 pontos de escrita relevantes (UC33, UC34,
 * UC36, UC38, UC39, UC40, UC41, UC43, UC44) publicam DashboardRecalculationRequestedEvent
 * com o atleta certo. A entrega efetiva na fila (AMQP) só acontece depois
 * do commit da transação principal (DashboardRecalculationEventPublisher,
 * @TransactionalEventListener AFTER_COMMIT) — que nunca ocorre de verdade
 * numa classe de teste @Transactional (rollback automático). Por isso
 * esses testes verificam a camada de baixo: o evento interno do Spring é
 * publicado de forma síncrona no momento da chamada, independente de
 * commit — é justamente esse evento que o publisher relay pega pra
 * mandar pra fila depois.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Transactional
class DashboardRecalculationEventPublishingTest {

    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldPublishEventWhenRegisteringSeries(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoregistrarserie@shottrack.com");
        UUID userId = currentUserId(token);
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        registerSeries(token, trainingId);

        assertEventPublishedFor(events, userId);
    }

    @Test
    void shouldPublishEventWhenUpdatingSeries(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoeditarserie@shottrack.com");
        UUID userId = currentUserId(token);
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);

        mockMvc.perform(patch("/api/series/" + seriesId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateSeriesRequest(null, null, null, null, null, "nota"))));

        assertEventPublishedFor(events, userId);
    }

    @Test
    void shouldPublishEventWhenRegisteringSeriesResult(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoregistrarresultado@shottrack.com");
        UUID userId = currentUserId(token);
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(tempoId, "12.3"))));

        assertEventPublishedFor(events, userId);
    }

    @Test
    void shouldPublishEventWhenRemovingSeriesResult(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoremoverresultado@shottrack.com");
        UUID userId = currentUserId(token);
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);
        UUID tempoId = resultTypeIdByName(token, "Tempo");

        mockMvc.perform(post("/api/series/" + seriesId + "/results")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterSeriesResultRequest(tempoId, "12.3"))));

        mockMvc.perform(delete("/api/series/" + seriesId + "/results/" + tempoId)
                .header("Authorization", "Bearer " + token));

        // registrar + remover: duas escritas, pelo menos dois eventos pro mesmo atleta
        assertThat(events.stream(DashboardRecalculationRequestedEvent.class)
                .filter(event -> event.userId().equals(userId))
                .count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldPublishEventWhenClosingTraining(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoencerrartreino@shottrack.com");
        UUID userId = currentUserId(token);
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        mockMvc.perform(post("/api/trainings/" + trainingId + "/closure")
                .header("Authorization", "Bearer " + token));

        assertEventPublishedFor(events, userId);
    }

    @Test
    void shouldPublishEventWhenClosingVisit(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoencerrarvisita@shottrack.com");
        UUID userId = currentUserId(token);
        UUID visitId = openVisit(token, "IPSC");

        mockMvc.perform(post("/api/visits/" + visitId + "/closure")
                .header("Authorization", "Bearer " + token));

        assertEventPublishedFor(events, userId);
    }

    @Test
    void shouldPublishEventWhenDeletingSeries(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoexcluirserie@shottrack.com");
        UUID userId = currentUserId(token);
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        UUID seriesId = registerSeries(token, trainingId);
        events.clear();

        mockMvc.perform(delete("/api/series/" + seriesId)
                .header("Authorization", "Bearer " + token));

        assertEventPublishedFor(events, userId);
    }

    @Test
    void shouldPublishEventWhenDeletingTraining(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoexcluirtreino@shottrack.com");
        UUID userId = currentUserId(token);
        UUID trainingId = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");

        mockMvc.perform(delete("/api/trainings/" + trainingId)
                .header("Authorization", "Bearer " + token));

        assertEventPublishedFor(events, userId);
    }

    @Test
    void shouldPublishEventWhenDeletingVisit(ApplicationEvents events) throws Exception {
        String token = registerAndLogin("atleta.eventoexcluirvisita@shottrack.com");
        UUID userId = currentUserId(token);
        UUID visitId = openVisit(token, "IPSC");

        mockMvc.perform(delete("/api/visits/" + visitId)
                .header("Authorization", "Bearer " + token));

        assertEventPublishedFor(events, userId);
    }

    private void assertEventPublishedFor(ApplicationEvents events, UUID userId) {
        List<DashboardRecalculationRequestedEvent> matches = events.stream(DashboardRecalculationRequestedEvent.class)
                .filter(event -> event.userId().equals(userId))
                .toList();
        assertThat(matches).isNotEmpty();
    }

    private UUID openVisit(String token, String modalityName) throws Exception {
        UUID modalityId = modalityIdByName(token, modalityName);

        mockMvc.perform(post("/api/practiced-modalities")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPracticedModalityRequest(modalityId))));

        var locationResult = mockMvc.perform(post("/api/training-locations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingLocationRegisterRequest("Clube de Tiro Central", "São Paulo", "SP"))))
                .andReturn();
        UUID trainingLocationId = idFromResponse(locationResult);

        var visitResult = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartVisitRequest(trainingLocationId, null))))
                .andReturn();
        return idFromResponse(visitResult);
    }

    private UUID registerSeries(String token, UUID trainingId) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterSeriesRequest(trainingId, null, null, null, null, null, null))))
                .andReturn();
        return idFromResponse(result);
    }

    private UUID currentUserId(String token) throws Exception {
        var result = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andReturn();
        return idFromResponse(result);
    }

    private UUID modalityIdByName(String token, String name) throws Exception {
        return idByName(token, "/api/modality-catalog", name);
    }

    private UUID resultTypeIdByName(String token, String name) throws Exception {
        return idByName(token, "/api/result-type-catalog", name);
    }

    private UUID idByName(String token, String path, String name) throws Exception {
        var result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token)).andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode item : items) {
            if (item.get("name").asText().equals(name)) {
                return UUID.fromString(item.get("id").asText());
            }
        }
        throw new AssertionError("Item não encontrado no catálogo (" + path + "): " + name);
    }

    private UUID idFromResponse(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta Teste", email, PASSWORD);
    }
}
