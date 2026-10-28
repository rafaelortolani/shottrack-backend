package com.shottrack.backend.application.dashboard.dto;

import java.util.UUID;

/**
 * UC42 v7: modalidade e tipo de resultado a abrir na mini evolução (UC46).
 */
public record SuggestedEvolutionResponse(
        UUID modalityId,
        UUID resultTypeId
) {
}
