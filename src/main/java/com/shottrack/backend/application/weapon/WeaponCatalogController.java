package com.shottrack.backend.application.weapon;

import com.shottrack.backend.application.weapon.dto.WeaponBrandResponse;
import com.shottrack.backend.application.weapon.dto.WeaponCaliberResponse;
import com.shottrack.backend.application.weapon.dto.WeaponModelResponse;
import com.shottrack.backend.application.weapon.dto.WeaponTypeResponse;
import com.shottrack.backend.application.weapon.usecase.WeaponCatalogService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/weapon-catalog")
@RequiredArgsConstructor
public class WeaponCatalogController {

    private final WeaponCatalogService weaponCatalogService;

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<WeaponTypeResponse>>> listTypes() {
        return ResponseEntity.ok(ApiResponse.ok(weaponCatalogService.listTypes()));
    }

    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<WeaponBrandResponse>>> listBrands() {
        return ResponseEntity.ok(ApiResponse.ok(weaponCatalogService.listBrands()));
    }

    @GetMapping("/brands/{brandId}/models")
    public ResponseEntity<ApiResponse<List<WeaponModelResponse>>> listModels(@PathVariable UUID brandId) {
        return ResponseEntity.ok(ApiResponse.ok(weaponCatalogService.listModelsByBrand(brandId)));
    }

    @GetMapping("/calibers")
    public ResponseEntity<ApiResponse<List<WeaponCaliberResponse>>> listCalibers() {
        return ResponseEntity.ok(ApiResponse.ok(weaponCatalogService.listCalibers()));
    }
}
