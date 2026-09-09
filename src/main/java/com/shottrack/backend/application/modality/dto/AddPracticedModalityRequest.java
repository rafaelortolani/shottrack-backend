package com.shottrack.backend.application.modality.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddPracticedModalityRequest(
        @NotNull(message = "modalidade é obrigatória")
        UUID modalityId
) {
}
