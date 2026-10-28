package com.shottrack.backend.application.visit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de data da visita (UC31 startedAt, UC34, UC59 — ADR-0024).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
abstract class VisitDatesIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PendingRegistrationRepository pendingRegistrationRepository;

    protected String token;
    protected UUID locationId;

    @BeforeEach
    void registerAthleteWithLocation() throws Exception {
        token = registerAndLogin("atleta.datasvisita@shottrack.com");
        locationId = newLocation(token);
    }

    protected String registerAndLogin(String email) throws Exception {
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Atleta", email, "senha12345");
    }

    protected UUID newLocation(String athleteToken) throws Exception {
        var result = mockMvc.perform(post("/api/training-locations")
                .header("Authorization", "Bearer " + athleteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Clube\",\"city\":\"São Paulo\",\"state\":\"SP\"}")).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    protected ResultActions start(String athleteToken, UUID location, Instant startedAt) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trainingLocationId", location);
        if (startedAt != null) {
            body.put("startedAt", startedAt.toString());
        }
        return mockMvc.perform(post("/api/visits")
                .header("Authorization", "Bearer " + athleteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected UUID startVisit(Instant startedAt) throws Exception {
        return idOf(start(token, locationId, startedAt).andExpect(status().isCreated()));
    }

    protected UUID startClosedVisit(Instant startedAt) throws Exception {
        UUID id = startVisit(startedAt);
        close(token, id).andExpect(status().isOk());
        return id;
    }

    protected ResultActions close(String athleteToken, UUID visitId) throws Exception {
        return mockMvc.perform(post("/api/visits/" + visitId + "/closure").header("Authorization", "Bearer " + athleteToken));
    }

    protected ResultActions editDates(String athleteToken, UUID visitId, String json) throws Exception {
        return mockMvc.perform(patch("/api/visits/" + visitId + "/dates")
                .header("Authorization", "Bearer " + athleteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected ResultActions editDates(UUID visitId, Instant startedAt, Instant endedAt) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (startedAt != null) {
            body.put("startedAt", startedAt.toString());
        }
        if (endedAt != null) {
            body.put("endedAt", endedAt.toString());
        }
        return editDates(token, visitId, objectMapper.writeValueAsString(body));
    }

    protected JsonNode visitFromList(String athleteToken, UUID visitId) throws Exception {
        var result = mockMvc.perform(get("/api/visits").header("Authorization", "Bearer " + athleteToken)).andReturn();
        for (JsonNode visit : objectMapper.readTree(result.getResponse().getContentAsString()).get("data")) {
            if (visit.get("id").asText().equals(visitId.toString())) {
                return visit;
            }
        }
        throw new AssertionError("Visita não listada: " + visitId);
    }

    protected UUID idOf(ResultActions result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    protected static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /** Meio-dia UTC do dia informado. */
    protected static Instant noon(LocalDate day) {
        return day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC);
    }

    /** Primeiro instante aceito para o início: 00:00 UTC de hoje menos 12 meses. */
    protected static Instant oldestAllowedStart() {
        return today().minusMonths(12).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
