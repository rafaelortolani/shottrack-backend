package com.shottrack.backend.application.ammunition.dto;

import com.shottrack.backend.application.weapon.dto.WeaponCaliberResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AmmunitionResponse(
        UUID id,
        String nickname,
        AmmunitionManufacturerResponse manufacturer,
        WeaponCaliberResponse caliber,
        BigDecimal projectileWeightGrains,
        BigDecimal powderCharge,
        String projectileType,
        String lot,
        String notes,
        Instant createdAt
) {
}
