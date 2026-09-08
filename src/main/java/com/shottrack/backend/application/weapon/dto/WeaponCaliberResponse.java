package com.shottrack.backend.application.weapon.dto;

import java.util.UUID;

public record WeaponCaliberResponse(
        UUID id,
        String name
) {
}
