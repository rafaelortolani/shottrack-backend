package com.shottrack.backend.application.dashboard.gateway;

import com.shottrack.backend.application.dashboard.model.DashboardSummary;

import java.util.Optional;
import java.util.UUID;

public interface DashboardSummaryGateway {

    DashboardSummary save(DashboardSummary summary);

    Optional<DashboardSummary> findByUserId(UUID userId);
}
