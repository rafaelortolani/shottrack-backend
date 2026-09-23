package com.shottrack.backend.application.dashboard;

import com.shottrack.backend.application.dashboard.dto.DashboardResponse;
import com.shottrack.backend.application.dashboard.usecase.DashboardService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> get(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(userId)));
    }
}
