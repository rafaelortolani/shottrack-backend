package com.shottrack.backend.application.visit.dto;

import com.shottrack.backend.application.visit.model.CompetitionScope;
import com.shottrack.backend.application.visit.model.VisitType;
import jakarta.validation.constraints.NotNull;

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

        CompetitionScope competitionScope
) {

    public StartVisitRequest(UUID trainingLocationId, String observations) {
        this(trainingLocationId, observations, null, null);
    }
}
