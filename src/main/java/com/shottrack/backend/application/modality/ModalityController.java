package com.shottrack.backend.application.modality;

import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.modality.usecase.ModalityCatalogService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/modality-catalog")
@RequiredArgsConstructor
public class ModalityController {

    private final ModalityCatalogService modalityCatalogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModalityResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(modalityCatalogService.listAll()));
    }
}
