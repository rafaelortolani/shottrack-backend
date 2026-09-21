package com.shottrack.backend.application.modality;

import com.shottrack.backend.application.modality.dto.ResultTypeResponse;
import com.shottrack.backend.application.modality.usecase.ResultTypeCatalogService;
import com.shottrack.backend.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/result-type-catalog")
@RequiredArgsConstructor
public class ResultTypeCatalogController {

    private final ResultTypeCatalogService resultTypeCatalogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResultTypeResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(resultTypeCatalogService.listAll()));
    }
}
