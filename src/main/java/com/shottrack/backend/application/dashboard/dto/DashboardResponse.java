package com.shottrack.backend.application.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record DashboardResponse(
        // UC42: ausente (não nulo) quando não há pendência de onboarding
        @JsonInclude(JsonInclude.Include.NON_NULL)
        OnboardingResponse onboarding,
        MainActionResponse mainAction,
        IndicatorResponse trainingsThisMonth,
        IndicatorResponse shotsThisMonth,
        IndicatorResponse modalitiesPracticed,
        List<RecentTrainingResponse> recentTrainings,
        List<ResultRecordResponse> records,
        // UC42 v7: sempre presente; null quando não há dados no filtro atual
        SuggestedEvolutionResponse suggestedEvolution
) {
}
