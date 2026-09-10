package com.shottrack.backend.application.ammunition.dto;

import java.util.UUID;

public record AmmunitionManufacturerResponse(
        UUID id,
        String name
) {
}
