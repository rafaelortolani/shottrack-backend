package com.shottrack.backend.application.weapon;

import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import com.shottrack.backend.application.weapon.dto.WeaponUpdateRequest;
import com.shottrack.backend.application.weapon.usecase.WeaponService;
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
@RequestMapping("/api/weapons")
@RequiredArgsConstructor
public class WeaponController {

    private final WeaponService weaponService;

    @PostMapping
    public ResponseEntity<ApiResponse<WeaponResponse>> register(Authentication authentication,
                                                                   @Valid @RequestBody WeaponRegisterRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        WeaponResponse response = weaponService.register(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WeaponResponse>>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<WeaponResponse> response = weaponService.listByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WeaponResponse>> update(Authentication authentication, @PathVariable UUID id,
                                                                 @Valid @RequestBody WeaponUpdateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        WeaponResponse response = weaponService.update(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        weaponService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
