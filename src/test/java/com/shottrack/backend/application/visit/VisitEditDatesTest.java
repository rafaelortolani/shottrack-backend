package com.shottrack.backend.application.visit;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.event.ApplicationEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC59 — editar datas da visita (ADR-0024). */
class VisitEditDatesTest extends VisitDatesIntegrationTest {

    private Instant endOf(UUID id) throws Exception {
        String ended = visitFromList(token, id).get("endedAt").asText();
        return Instant.parse(ended);
    }

    @Test
    void shouldEditOnlyStartOfClosedVisitKeepingTheEnd() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(1)));
        Instant endBefore = endOf(id);
        Instant newStart = noon(today().minusDays(5));

        editDates(id, newStart, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startedAt").value(newStart.toString()))
                .andExpect(jsonPath("$.data.endedAt").value(endBefore.toString()))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void shouldEditOnlyEndOfClosedVisitKeepingTheStart() throws Exception {
        Instant start = noon(today().minusDays(3));
        UUID id = startClosedVisit(start);
        Instant newEnd = start.plus(Duration.ofHours(2));

        editDates(id, null, newEnd)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startedAt").value(start.toString()))
                .andExpect(jsonPath("$.data.endedAt").value(newEnd.toString()));
    }

    @Test
    void shouldEditBothDatesOfClosedVisit() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(1)));
        Instant newStart = noon(today().minusDays(8));
        Instant newEnd = newStart.plus(Duration.ofHours(3));

        editDates(id, newStart, newEnd)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startedAt").value(newStart.toString()))
                .andExpect(jsonPath("$.data.endedAt").value(newEnd.toString()));
        assertThat(visitFromList(token, id).get("startedAt").asText()).isEqualTo(newStart.toString());
    }

    @Test
    void shouldEditStartOfVisitInProgress() throws Exception {
        UUID id = startVisit(null);
        Instant newStart = noon(today().minusDays(2));

        editDates(id, newStart, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startedAt").value(newStart.toString()))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.endedAt").isEmpty());
    }

    @Test
    void shouldAcceptEqualStartAndEnd() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(2)));
        Instant moment = noon(today().minusDays(2));

        editDates(id, moment, moment).andExpect(status().isOk());
    }

    @Test
    void shouldRejectEndedAtOnVisitInProgress() throws Exception {
        UUID id = startVisit(noon(today().minusDays(2)));

        editDates(id, null, Instant.now().minusSeconds(60))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        editDates(id, noon(today().minusDays(3)), Instant.now().minusSeconds(60))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        assertThat(visitFromList(token, id).get("startedAt").asText()).isEqualTo(noon(today().minusDays(2)).toString());
    }

    @Test
    void shouldRejectEmptyBody() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(2)));

        editDates(token, id, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        editDates(token, id, "{\"startedAt\":null,\"endedAt\":null}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMalformedDate() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(2)));

        editDates(token, id, "{\"startedAt\":\"ontem\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectFutureStartAndFutureEnd() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(2)));
        Instant future = Instant.now().plus(Duration.ofDays(1));

        editDates(id, future, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_IN_FUTURE"));
        editDates(id, null, future).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_IN_FUTURE"));
    }

    @Test
    void shouldToleratePlusAFewMinutesOfClockSkewOnEnd() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(2)));

        editDates(id, null, Instant.now().plus(Duration.ofMinutes(2))).andExpect(status().isOk());
        assertThat(endOf(id)).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void shouldAcceptStartAtTheTwelveMonthLimitAndRejectOneSecondBefore() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(2)));
        // o fim fica em "hoje"; só o início muda
        editDates(id, oldestAllowedStart(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startedAt").value(oldestAllowedStart().toString()));

        editDates(id, oldestAllowedStart().minusSeconds(1), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_TOO_OLD"));
    }

    @Test
    void shouldRejectStartAfterTheEnd() throws Exception {
        Instant start = noon(today().minusDays(3));
        UUID id = startClosedVisit(start);
        Instant end = start.plus(Duration.ofHours(1));
        editDates(id, null, end).andExpect(status().isOk());

        // novo início depois do fim existente
        editDates(id, end.plusSeconds(1), null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_RANGE_INVALID"));
        // novo fim antes do início existente
        editDates(id, null, start.minusSeconds(1)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_RANGE_INVALID"));
        // os dois, na ordem errada
        editDates(id, noon(today().minusDays(1)), noon(today().minusDays(2))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VISIT_DATE_RANGE_INVALID"));
    }

    @Test
    void shouldKeepVisitUnchangedWhenValidationFails() throws Exception {
        Instant start = noon(today().minusDays(3));
        UUID id = startClosedVisit(start);
        Instant endBefore = endOf(id);

        editDates(id, Instant.now().plus(Duration.ofDays(1)), noon(today().minusDays(10))).andExpect(status().isBadRequest());

        assertThat(visitFromList(token, id).get("startedAt").asText()).isEqualTo(start.toString());
        assertThat(endOf(id)).isEqualTo(endBefore);
    }

    @Test
    void shouldNotChangeTrainingTimes() throws Exception {
        UUID id = startVisit(noon(today().minusDays(1)));
        UUID trainingId = com.shottrack.backend.support.SeriesTestSupport
                .openTrainingInVisit(mockMvc, objectMapper, token, id, "IPSC");
        String trainingStart = visitFromList(token, id).get("trainings").get(0).get("startedAt").asText();

        editDates(id, noon(today().minusDays(6)), null).andExpect(status().isOk());

        var training = visitFromList(token, id).get("trainings").get(0);
        assertThat(training.get("id").asText()).isEqualTo(trainingId.toString());
        assertThat(training.get("startedAt").asText()).isEqualTo(trainingStart);
    }

    @Test
    void shouldPublishDashboardRecalculationAfterEditing(ApplicationEvents events) throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(1)));
        UUID userId = currentUserId();
        long before = events.stream(DashboardRecalculationRequestedEvent.class)
                .filter(event -> event.userId().equals(userId)).count();

        editDates(id, noon(today().minusDays(4)), null).andExpect(status().isOk());

        assertThat(events.stream(DashboardRecalculationRequestedEvent.class)
                .filter(event -> event.userId().equals(userId)).count()).isEqualTo(before + 1);
    }

    @Test
    void shouldNotPublishRecalculationWhenEditFails(ApplicationEvents events) throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(1)));
        UUID userId = currentUserId();
        long before = events.stream(DashboardRecalculationRequestedEvent.class)
                .filter(event -> event.userId().equals(userId)).count();

        editDates(token, id, "{}").andExpect(status().isBadRequest());

        assertThat(events.stream(DashboardRecalculationRequestedEvent.class)
                .filter(event -> event.userId().equals(userId)).count()).isEqualTo(before);
    }

    @Test
    void shouldRejectUnknownVisit() throws Exception {
        editDates(UUID.randomUUID(), noon(today().minusDays(1)), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
    }

    @Test
    void shouldRejectVisitOfAnotherAthleteAndLeaveItUntouched() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(2)));
        String other = registerAndLogin("outro.datasvisita@shottrack.com");

        editDates(other, id, "{\"startedAt\":\"" + noon(today().minusDays(5)) + "\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));

        assertThat(visitFromList(token, id).get("startedAt").asText()).isEqualTo(noon(today().minusDays(2)).toString());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/visits/" + UUID.randomUUID() + "/dates")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"startedAt\":\"" + noon(today().minusDays(1)) + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldTranslateRangeErrorToEnglish() throws Exception {
        UUID id = startClosedVisit(noon(today().minusDays(3)));

        mockMvc.perform(patch("/api/visits/" + id + "/dates")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "en")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"startedAt\":\"" + noon(today().minusDays(1)) + "\",\"endedAt\":\"" + noon(today().minusDays(2)) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("The visit start cannot be after its end"));
    }

    private UUID currentUserId() throws Exception {
        var result = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }
}
