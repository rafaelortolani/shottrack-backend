package com.shottrack.backend.application.series;

import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.SeriesHistoryRequest;
import com.shottrack.backend.application.series.dto.SeriesHistoryResponse;
import com.shottrack.backend.application.series.dto.SeriesResponse;
import com.shottrack.backend.application.series.dto.UpdateSeriesRequest;
import com.shottrack.backend.application.series.usecase.SeriesHistoryService;
import com.shottrack.backend.application.series.usecase.SeriesService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;
    private final SeriesHistoryService seriesHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeriesHistoryResponse>>> history(Authentication authentication,
                                                                            @Valid @ModelAttribute SeriesHistoryRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(seriesHistoryService.getHistory(userId, request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SeriesResponse>> register(Authentication authentication,
                                                                   @Valid @RequestBody RegisterSeriesRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        SeriesResponse response = seriesService.register(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SeriesResponse>> update(Authentication authentication, @PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateSeriesRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        SeriesResponse response = seriesService.update(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        seriesService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
