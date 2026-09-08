package com.shottrack.backend.application.weapon.dto;

import java.time.Instant;
import java.util.UUID;

public record WeaponResponse(
        UUID id,
        WeaponTypeResponse type,
        WeaponBrandResponse brand,
        WeaponModelResponse model,
        WeaponCaliberResponse caliber,
        Instant createdAt
) {
}
