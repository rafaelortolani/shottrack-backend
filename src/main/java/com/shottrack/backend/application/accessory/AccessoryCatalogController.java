package com.shottrack.backend.application.accessory;

import com.shottrack.backend.application.accessory.dto.AccessoryTypeResponse;
import com.shottrack.backend.application.accessory.usecase.AccessoryCatalogService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accessory-catalog")
@RequiredArgsConstructor
public class AccessoryCatalogController {

    private final AccessoryCatalogService accessoryCatalogService;

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<AccessoryTypeResponse>>> listTypes() {
        return ResponseEntity.ok(ApiResponse.ok(accessoryCatalogService.listTypes()));
    }
}
