package com.shottrack.backend.application.modality;

import com.shottrack.backend.application.modality.dto.AddModalityResultTypeRequest;
import com.shottrack.backend.application.modality.dto.ResultTypeResponse;
import com.shottrack.backend.application.modality.usecase.ModalityResultTypeService;
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
@RequestMapping("/api/practiced-modalities/{modalityId}/result-types")
@RequiredArgsConstructor
public class ModalityResultTypeController {

    private final ModalityResultTypeService modalityResultTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResultTypeResponse>>> list(Authentication authentication, @PathVariable UUID modalityId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(modalityResultTypeService.listConfigured(userId, modalityId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResultTypeResponse>> add(Authentication authentication, @PathVariable UUID modalityId,
                                                                  @Valid @RequestBody AddModalityResultTypeRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        ResultTypeResponse response = modalityResultTypeService.add(userId, modalityId, request.resultTypeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/{resultTypeId}")
    public ResponseEntity<ApiResponse<Void>> remove(Authentication authentication, @PathVariable UUID modalityId,
                                                       @PathVariable UUID resultTypeId) {
        UUID userId = (UUID) authentication.getPrincipal();
        modalityResultTypeService.remove(userId, modalityId, resultTypeId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
