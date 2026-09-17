package com.shottrack.backend.application.traininglocation;

import com.shottrack.backend.application.traininglocation.dto.TrainingLocationRegisterRequest;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationResponse;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationUpdateRequest;
import com.shottrack.backend.application.traininglocation.usecase.TrainingLocationService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/training-locations")
@RequiredArgsConstructor
public class TrainingLocationController {

    private final TrainingLocationService trainingLocationService;

    @PostMapping
    public ResponseEntity<ApiResponse<TrainingLocationResponse>> register(Authentication authentication,
                                                                            @Valid @RequestBody TrainingLocationRegisterRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        TrainingLocationResponse response = trainingLocationService.register(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainingLocationResponse>>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<TrainingLocationResponse> response = trainingLocationService.listByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainingLocationResponse>> update(Authentication authentication, @PathVariable UUID id,
                                                                          @Valid @RequestBody TrainingLocationUpdateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        TrainingLocationResponse response = trainingLocationService.update(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        trainingLocationService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
