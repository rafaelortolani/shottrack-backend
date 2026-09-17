package com.shottrack.backend.application.accessory.dto;

import java.util.UUID;

public record AccessoryTypeResponse(
        UUID id,
        String name
) {
}
