package com.shottrack.backend.application.dashboard;

import com.shottrack.backend.application.dashboard.dto.DashboardResponse;
import com.shottrack.backend.application.dashboard.dto.EvolutionPointResponse;
import com.shottrack.backend.application.dashboard.dto.EvolutionRequest;
import com.shottrack.backend.application.dashboard.usecase.DashboardService;
import com.shottrack.backend.application.dashboard.usecase.EvolutionService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final EvolutionService evolutionService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> get(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(userId)));
    }

    @GetMapping("/evolution")
    public ResponseEntity<ApiResponse<List<EvolutionPointResponse>>> evolution(Authentication authentication,
                                                                               @Valid @ModelAttribute EvolutionRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(evolutionService.getEvolution(userId, request)));
    }
}
