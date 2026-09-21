package com.shottrack.backend.application.visit;

import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.application.visit.dto.VisitResponse;
import com.shottrack.backend.application.visit.usecase.VisitService;
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
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping
    public ResponseEntity<ApiResponse<VisitResponse>> start(Authentication authentication,
                                                                @Valid @RequestBody StartVisitRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        VisitResponse response = visitService.start(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VisitResponse>>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(visitService.listByUser(userId)));
    }

    @PostMapping("/{visitId}/closure")
    public ResponseEntity<ApiResponse<VisitResponse>> close(Authentication authentication, @PathVariable UUID visitId) {
        UUID userId = (UUID) authentication.getPrincipal();
        VisitResponse response = visitService.close(userId, visitId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
