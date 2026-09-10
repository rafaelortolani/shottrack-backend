package com.shottrack.backend.application.ammunition;

import com.shottrack.backend.application.ammunition.dto.AmmunitionRegisterRequest;
import com.shottrack.backend.application.ammunition.dto.AmmunitionResponse;
import com.shottrack.backend.application.ammunition.dto.AmmunitionUpdateRequest;
import com.shottrack.backend.application.ammunition.usecase.AmmunitionService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/ammunitions")
@RequiredArgsConstructor
public class AmmunitionController {

    private final AmmunitionService ammunitionService;

    @PostMapping
    public ResponseEntity<ApiResponse<AmmunitionResponse>> register(Authentication authentication,
                                                                       @Valid @RequestBody AmmunitionRegisterRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AmmunitionResponse response = ammunitionService.register(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AmmunitionResponse>>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<AmmunitionResponse> response = ammunitionService.listByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AmmunitionResponse>> update(Authentication authentication, @PathVariable UUID id,
                                                                     @Valid @RequestBody AmmunitionUpdateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AmmunitionResponse response = ammunitionService.update(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
