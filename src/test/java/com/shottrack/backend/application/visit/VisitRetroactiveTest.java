package com.shottrack.backend.application.visit;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC31 (startedAt) e UC34 (encerrar visita retroativa) — ADR-0024. */
class VisitRetroactiveTest extends VisitDatesIntegrationTest {

    @Test
    void shouldStartVisitNowWhenStartedAtIsOmitted() throws Exception {
        Instant before = Instant.now();

        var result = start(token, locationId, null).andExpect(status().isCreated()).andReturn();
        Instant startedAt = Instant.parse(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("startedAt").asText());

        assertThat(startedAt).isBetween(before.minusSeconds(5), Instant.now().plusSeconds(5));
    }

    @Test
    void shouldStartRetroactiveVisitAndKeepTheInformedDate() throws Exception {
        Instant twoDaysAgo = noon(today().minusDays(2));

        start(token, locationId, twoDaysAgo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.startedAt").value(twoDaysAgo.toString()))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.endedAt").isEmpty());
    }

    @Test
    void shouldListRetroactiveVisitWithTheInformedDate() throws Exception {
        Instant lastMonth = noon(today().minusMonths(1));
        UUID id = startVisit(lastMonth);

        assertThat(visitFromList(token, id).get("startedAt").asText()).isEqualTo(lastMonth.toString());
    }

    @Test
    void shouldAcceptDateWithUtcOffset() throws Exception {
        String local = today().minusDays(3) + "T10:00:00-03:00";

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingLocationId\":\"" + locationId + "\",\"startedAt\":\"" + local + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.startedAt").value(today().minusDays(3) + "T13:00:00Z"));
    }

    @Test
    void shouldAcceptStartExactlyAtTheTwelveMonthLimit() throws Exception {
        start(token, locationId, oldestAllowedStart())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.startedAt").value(oldestAllowedStart().toString()));
    }

    @Test
    void shouldRejectStartOneSecondBeyondTheTwelveMonthLimit() throws Exception {
        start(token, locationId, oldestAllowedStart().minusSeconds(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_TOO_OLD"))
                .andExpect(jsonPath("$.error.message").value("A data de início da visita não pode ser anterior a 12 meses"));
    }

    @Test
    void shouldRejectVeryOldStart() throws Exception {
        start(token, locationId, noon(today().minusYears(5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_TOO_OLD"));
    }

    @Test
    void shouldRejectFutureStart() throws Exception {
        start(token, locationId, Instant.now().plus(Duration.ofHours(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_IN_FUTURE"));
        start(token, locationId, noon(today().plusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_IN_FUTURE"));
    }

    @Test
    void shouldTranslateDateErrorsToEnglish() throws Exception {
        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingLocationId\":\"" + locationId + "\",\"startedAt\":\""
                                + Instant.now().plus(Duration.ofDays(1)) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("The visit date cannot be in the future"));
    }

    @Test
    void shouldToleratePlusAFewMinutesOfClientClockSkewAndClampToNow() throws Exception {
        Instant skewed = Instant.now().plus(Duration.ofMinutes(2));

        var result = start(token, locationId, skewed).andExpect(status().isCreated()).andReturn();
        Instant stored = Instant.parse(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("startedAt").asText());

        assertThat(stored).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void shouldRejectStartBeyondTheClockTolerance() throws Exception {
        start(token, locationId, Instant.now().plus(Duration.ofMinutes(10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_IN_FUTURE"));
    }

    @Test
    void shouldRejectMalformedOrZonelessDate() throws Exception {
        for (String invalid : new String[]{"\"ontem\"", "\"2026-10-01T10:00:00\"", "123abc"}) {
            mockMvc.perform(post("/api/visits")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"trainingLocationId\":\"" + locationId + "\",\"startedAt\":" + invalid + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void shouldNotCreateVisitWhenDateIsInvalid() throws Exception {
        start(token, locationId, Instant.now().plus(Duration.ofDays(2))).andExpect(status().isBadRequest());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/visits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldValidateLocationBeforeAcceptingAnyDate() throws Exception {
        start(token, UUID.randomUUID(), noon(today().minusDays(1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRAINING_LOCATION_NOT_FOUND"));
    }

    @Test
    void shouldPublishDashboardRecalculationWhenStartingRetroactiveVisit(ApplicationEvents events) throws Exception {
        UUID userId = currentUserId();

        startVisit(noon(today().minusDays(4)));

        assertThat(events.stream(DashboardRecalculationRequestedEvent.class)
                .filter(event -> event.userId().equals(userId))).isNotEmpty();
    }

    @Test
    void shouldCloseRetroactiveVisitWithEndedAtEqualToNow() throws Exception {
        UUID id = startVisit(noon(today().minusDays(10)));
        Instant before = Instant.now();

        var result = close(token, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.startedAt").value(noon(today().minusDays(10)).toString()))
                .andReturn();
        Instant endedAt = Instant.parse(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("endedAt").asText());

        assertThat(endedAt).isBetween(before.minusSeconds(5), Instant.now().plusSeconds(5));
    }

    private UUID currentUserId() throws Exception {
        var result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/me")
                .header("Authorization", "Bearer " + token)).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }
}
