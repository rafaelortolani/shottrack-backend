package com.shottrack.backend.application.weapon.dto;

import java.util.UUID;

public record WeaponModelResponse(
        UUID id,
        String name
) {
}
