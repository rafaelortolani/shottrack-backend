package com.shottrack.backend.application.dashboard.dto;

import java.util.List;

public record WeaponCollectionResponse(
        int weaponCount,
        List<String> weaponNames
) {
}
