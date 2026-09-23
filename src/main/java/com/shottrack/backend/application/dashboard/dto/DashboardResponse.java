package com.shottrack.backend.application.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record DashboardResponse(
        // UC42: ausente (não nulo) quando não há pendência de onboarding
        @JsonInclude(JsonInclude.Include.NON_NULL)
        OnboardingResponse onboarding,
        MainActionResponse mainAction,
        int trainingsThisMonth,
        int shotsThisMonth,
        List<String> practicedModalities,
        List<RecentTrainingResponse> recentTrainings,
        List<ModalitySummaryResponse> modalitySummaries,
        WeaponCollectionResponse weaponCollection,
        DashboardHighlightResponse highlight
) {
}
