package com.shottrack.backend.application.ammunition.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Edição parcial (UC15/ADR-0007): campo ausente/null significa "não alterar" —
 * diferente da edição de arma, que substitui tudo.
 */
public record AmmunitionUpdateRequest(
        UUID manufacturerId,
        UUID caliberId,
        String nickname,
        BigDecimal projectileWeightGrains,
        BigDecimal powderCharge,
        String projectileType,
        String lot,
        String notes
) {
}
