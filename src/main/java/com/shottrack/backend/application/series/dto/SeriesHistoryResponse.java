package com.shottrack.backend.application.series.dto;

import com.shottrack.backend.application.ammunition.dto.AmmunitionResponse;
import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationResponse;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC47: série com o contexto completo — local vem da visita, modalidade do
 * treino. date é o dia do treino (mesmo critério do filtro de período).
 */
public record SeriesHistoryResponse(
        UUID id,
        UUID trainingId,
        LocalDate date,
        TrainingLocationResponse trainingLocation,
        ModalityResponse modality,
        WeaponResponse weapon,
        AmmunitionResponse ammunition,
        BigDecimal distanceMeters,
        String target,
        Integer shotCount,
        String notes,
        Instant createdAt,
        List<SeriesResultResponse> results
) {
}
