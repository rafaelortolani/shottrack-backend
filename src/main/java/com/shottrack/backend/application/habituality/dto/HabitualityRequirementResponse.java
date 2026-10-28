package com.shottrack.backend.application.habituality.dto;

import com.shottrack.backend.application.habituality.model.ActivityType;
import com.shottrack.backend.application.visit.model.CompetitionScope;

public record HabitualityRequirementResponse(
        ActivityType activityType,
        int minQuantity,
        boolean onlyInClub,
        boolean distinctEvents,
        CompetitionScope minScope
) {
}
