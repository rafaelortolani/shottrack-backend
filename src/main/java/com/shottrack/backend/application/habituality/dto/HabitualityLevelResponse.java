package com.shottrack.backend.application.habituality.dto;

import java.util.List;

public record HabitualityLevelResponse(
        String name,
        int position,
        List<HabitualityRequirementResponse> requirements
) {
}
