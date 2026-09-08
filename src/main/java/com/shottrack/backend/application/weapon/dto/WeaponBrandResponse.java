package com.shottrack.backend.application.weapon.dto;

import java.util.UUID;

public record WeaponBrandResponse(
        UUID id,
        String name
) {
}
