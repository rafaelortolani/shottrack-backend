package com.shottrack.backend.application.ammunition;

import com.shottrack.backend.application.ammunition.dto.AmmunitionManufacturerResponse;
import com.shottrack.backend.application.ammunition.usecase.AmmunitionCatalogService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ammunition-catalog")
@RequiredArgsConstructor
public class AmmunitionCatalogController {

    private final AmmunitionCatalogService ammunitionCatalogService;

    @GetMapping("/manufacturers")
    public ResponseEntity<ApiResponse<List<AmmunitionManufacturerResponse>>> listManufacturers() {
        return ResponseEntity.ok(ApiResponse.ok(ammunitionCatalogService.listManufacturers()));
    }
}
