package com.shottrack.backend.application.accessory.dto;

import com.shottrack.backend.application.weapon.dto.WeaponResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccessoryResponse(
        UUID id,
        String name,
        String type,
        String notes,
        List<WeaponResponse> weapons,
        Instant createdAt
) {
}
