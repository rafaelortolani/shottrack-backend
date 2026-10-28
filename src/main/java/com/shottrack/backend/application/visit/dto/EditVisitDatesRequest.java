package com.shottrack.backend.application.visit.dto;

import java.time.Instant;

/**
 * UC59: edição parcial — campo ausente/null significa "não alterar". "Pelo menos um campo" e
 * "endedAt só em visita encerrada" são regras do usecase (dependem do estado da visita).
 */
public record EditVisitDatesRequest(
        Instant startedAt,
        Instant endedAt
) {
}
