package com.shottrack.backend.application.dashboard.dto;

import com.shottrack.backend.application.dashboard.model.OnboardingStep;

import java.util.List;

public record OnboardingResponse(
        List<OnboardingStep> pendingSteps
) {
}
