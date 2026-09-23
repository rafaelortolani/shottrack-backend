package com.shottrack.backend.application.dashboard.gateway.repository;

import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DashboardSummaryRepository extends JpaRepository<DashboardSummary, UUID> {

    Optional<DashboardSummary> findByUserId(UUID userId);
}
