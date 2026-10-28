package com.shottrack.backend.application.habituality.dto;

import com.shottrack.backend.application.habituality.model.ActivityType;
import com.shottrack.backend.application.visit.model.CompetitionScope;

public record ProgressRequirementResponse(
        String description,
        ActivityType activityType,
        boolean onlyInClub,
        boolean distinctEvents,
        CompetitionScope minScope,
        int required,
        long done,
        long missing,
        boolean met
) {
}
