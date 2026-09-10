package com.shottrack.backend.application.accessory;

import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.usecase.AccessoryService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accessories")
@RequiredArgsConstructor
public class AccessoryController {

    private final AccessoryService accessoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccessoryResponse>> register(Authentication authentication,
                                                                       @Valid @RequestBody AccessoryRegisterRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AccessoryResponse response = accessoryService.register(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccessoryResponse>>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<AccessoryResponse> response = accessoryService.listByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
