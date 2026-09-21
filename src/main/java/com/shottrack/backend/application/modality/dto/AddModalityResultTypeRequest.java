package com.shottrack.backend.application.modality.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddModalityResultTypeRequest(
        @NotNull(message = "tipo de resultado é obrigatório")
        UUID resultTypeId
) {
}
