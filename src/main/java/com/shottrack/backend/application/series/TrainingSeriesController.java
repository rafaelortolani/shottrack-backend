package com.shottrack.backend.application.series;

import com.shottrack.backend.application.series.dto.SeriesResponse;
import com.shottrack.backend.application.series.usecase.SeriesService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trainings/{trainingId}/series")
@RequiredArgsConstructor
public class TrainingSeriesController {

    private final SeriesService seriesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeriesResponse>>> list(Authentication authentication, @PathVariable UUID trainingId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(seriesService.listByTraining(userId, trainingId)));
    }
}
