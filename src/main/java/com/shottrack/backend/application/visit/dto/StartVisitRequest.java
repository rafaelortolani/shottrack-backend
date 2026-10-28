package com.shottrack.backend.application.visit.dto;

import com.shottrack.backend.application.visit.model.CompetitionScope;
import com.shottrack.backend.application.visit.model.VisitType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * ADR-0023: visitType ausente = TREINO; competitionScope só vale com COMPETICAO
 * (validado no usecase). Valores fora do enum viram 400 na desserialização.
 */
public record StartVisitRequest(
        @NotNull(message = "local de treino é obrigatório")
        UUID trainingLocationId,

        String observations,

        VisitType visitType,

        CompetitionScope competitionScope,

        /** ADR-0024: ISO-8601 com fuso (ex.: 2026-10-01T10:00:00-03:00); ausente = agora. */
        Instant startedAt
) {

    public StartVisitRequest(UUID trainingLocationId, String observations) {
        this(trainingLocationId, observations, null, null, null);
    }
}
