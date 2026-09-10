package com.shottrack.backend.application.accessory;

import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.dto.AssociateAccessoryWeaponRequest;
import com.shottrack.backend.application.accessory.usecase.AccessoryWeaponService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/accessories/{accessoryId}/weapons")
@RequiredArgsConstructor
public class AccessoryWeaponController {

    private final AccessoryWeaponService accessoryWeaponService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccessoryResponse>> associate(Authentication authentication,
                                                                       @PathVariable UUID accessoryId,
                                                                       @Valid @RequestBody AssociateAccessoryWeaponRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AccessoryResponse response = accessoryWeaponService.associate(userId, accessoryId, request.weaponId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/{weaponId}")
    public ResponseEntity<ApiResponse<AccessoryResponse>> disassociate(Authentication authentication,
                                                                          @PathVariable UUID accessoryId,
                                                                          @PathVariable UUID weaponId) {
        UUID userId = (UUID) authentication.getPrincipal();
        AccessoryResponse response = accessoryWeaponService.disassociate(userId, accessoryId, weaponId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
