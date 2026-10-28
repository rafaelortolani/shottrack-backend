package com.shottrack.backend.application.visit.gateway;

import com.shottrack.backend.application.visit.model.CompetitionScope;
import com.shottrack.backend.application.visit.model.VisitType;

import java.time.Instant;
import java.util.UUID;

/**
 * ADR-0023: o que a apuração de habitualidade precisa de uma visita encerrada —
 * tipo, âmbito, se o local é clube e o par (início, local) dos eventos distintos.
 */
public record ClosedVisitFact(
        VisitType visitType,
        CompetitionScope competitionScope,
        boolean club,
        UUID trainingLocationId,
        Instant startedAt
) {
}
