package com.shottrack.backend.application.dashboard.gateway;

import com.shottrack.backend.application.dashboard.gateway.repository.DashboardSummaryRepository;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class DashboardSummaryGatewayImpl implements DashboardSummaryGateway {

    private final DashboardSummaryRepository dashboardSummaryRepository;

    @Override
    public DashboardSummary save(DashboardSummary summary) {
        return dashboardSummaryRepository.save(summary);
    }

    @Override
    public Optional<DashboardSummary> findByUserId(UUID userId) {
        return dashboardSummaryRepository.findByUserId(userId);
    }
}
