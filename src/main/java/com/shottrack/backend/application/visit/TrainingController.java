package com.shottrack.backend.application.visit;

import com.shottrack.backend.application.visit.dto.OpenTrainingRequest;
import com.shottrack.backend.application.visit.dto.TrainingResponse;
import com.shottrack.backend.application.visit.usecase.TrainingService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping
    public ResponseEntity<ApiResponse<TrainingResponse>> open(Authentication authentication,
                                                                 @Valid @RequestBody OpenTrainingRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        TrainingResponse response = trainingService.open(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/{trainingId}/closure")
    public ResponseEntity<ApiResponse<TrainingResponse>> close(Authentication authentication, @PathVariable UUID trainingId) {
        UUID userId = (UUID) authentication.getPrincipal();
        TrainingResponse response = trainingService.close(userId, trainingId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
