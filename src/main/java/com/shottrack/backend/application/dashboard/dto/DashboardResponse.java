package com.shottrack.backend.application.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        int trainingsThisMonth,
        int shotsThisMonth,
        List<String> practicedModalities,
        List<RecentVisitResponse> recentVisits,
        DashboardHighlightResponse highlight
) {
}
