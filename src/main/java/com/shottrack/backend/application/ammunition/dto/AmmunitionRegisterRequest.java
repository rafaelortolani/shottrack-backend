package com.shottrack.backend.application.ammunition.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AmmunitionRegisterRequest(
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
