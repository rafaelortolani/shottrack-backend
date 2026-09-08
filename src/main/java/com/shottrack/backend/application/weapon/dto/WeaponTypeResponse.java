package com.shottrack.backend.application.weapon.dto;

import java.util.UUID;

public record WeaponTypeResponse(
        UUID id,
        String name
) {
}
