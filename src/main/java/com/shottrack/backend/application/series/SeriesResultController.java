package com.shottrack.backend.application.series;

import com.shottrack.backend.application.series.dto.MarkSeriesResultNotApplicableRequest;
import com.shottrack.backend.application.series.dto.RegisterSeriesResultRequest;
import com.shottrack.backend.application.series.dto.SeriesResultResponse;
import com.shottrack.backend.application.series.usecase.SeriesResultService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/series/{seriesId}/results")
@RequiredArgsConstructor
public class SeriesResultController {

    private final SeriesResultService seriesResultService;

    @PostMapping
    public ResponseEntity<ApiResponse<SeriesResultResponse>> registerValue(Authentication authentication, @PathVariable UUID seriesId,
                                                                              @Valid @RequestBody RegisterSeriesResultRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        SeriesResultResponse response = seriesResultService.registerValue(userId, seriesId, request.resultTypeId(), request.value());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/not-applicable")
    public ResponseEntity<ApiResponse<SeriesResultResponse>> markNotApplicable(Authentication authentication, @PathVariable UUID seriesId,
                                                                                  @Valid @RequestBody MarkSeriesResultNotApplicableRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        SeriesResultResponse response = seriesResultService.markNotApplicable(userId, seriesId, request.resultTypeId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{resultTypeId}")
    public ResponseEntity<ApiResponse<Void>> remove(Authentication authentication, @PathVariable UUID seriesId,
                                                       @PathVariable UUID resultTypeId) {
        UUID userId = (UUID) authentication.getPrincipal();
        seriesResultService.remove(userId, seriesId, resultTypeId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
