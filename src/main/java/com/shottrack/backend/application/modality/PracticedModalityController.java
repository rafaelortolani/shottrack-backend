package com.shottrack.backend.application.modality;

import com.shottrack.backend.application.modality.dto.AddPracticedModalityRequest;
import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.modality.usecase.PracticedModalityService;
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
@RequestMapping("/api/practiced-modalities")
@RequiredArgsConstructor
public class PracticedModalityController {

    private final PracticedModalityService practicedModalityService;

    @PostMapping
    public ResponseEntity<ApiResponse<ModalityResponse>> add(Authentication authentication,
                                                                @Valid @RequestBody AddPracticedModalityRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        ModalityResponse response = practicedModalityService.add(userId, request.modalityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModalityResponse>>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(practicedModalityService.listByUser(userId)));
    }

    @DeleteMapping("/{modalityId}")
    public ResponseEntity<ApiResponse<Void>> remove(Authentication authentication, @PathVariable UUID modalityId) {
        UUID userId = (UUID) authentication.getPrincipal();
        practicedModalityService.remove(userId, modalityId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
