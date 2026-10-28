package com.shottrack.backend.application.visit;

import com.shottrack.backend.application.visit.dto.EditVisitDatesRequest;
import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.application.visit.dto.VisitResponse;
import com.shottrack.backend.application.visit.usecase.VisitService;
import com.shottrack.backend.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @Operation(summary = "Iniciar visita (UC31)",
            description = "startedAt opcional (ISO-8601 com fuso; padrão agora): não futuro (tolerância de 5 min de relógio) "
                    + "e não anterior a 12 meses. Erros: UNAUTHORIZED (401), TRAINING_LOCATION_NOT_FOUND (404), "
                    + "VISIT_DATE_IN_FUTURE, VISIT_DATE_TOO_OLD, VALIDATION_ERROR (400).")
    @PostMapping
    public ResponseEntity<ApiResponse<VisitResponse>> start(Authentication authentication,
                                                                @Valid @RequestBody StartVisitRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        VisitResponse response = visitService.start(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VisitResponse>>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(visitService.listByUser(userId)));
    }

    @PostMapping("/{visitId}/closure")
    public ResponseEntity<ApiResponse<VisitResponse>> close(Authentication authentication, @PathVariable UUID visitId) {
        UUID userId = (UUID) authentication.getPrincipal();
        VisitResponse response = visitService.close(userId, visitId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Editar datas da visita (UC59)",
            description = "Edição parcial de startedAt e/ou endedAt (ISO-8601 com fuso). Início: não futuro e não anterior a "
                    + "12 meses; fim: não futuro; início <= fim; endedAt só em visita encerrada. Dispara o recálculo do "
                    + "dashboard. Erros: UNAUTHORIZED (401), VISIT_NOT_FOUND (404), VISIT_DATE_IN_FUTURE, VISIT_DATE_TOO_OLD, "
                    + "VISIT_DATE_RANGE_INVALID, VALIDATION_ERROR (400).")
    @PatchMapping("/{visitId}/dates")
    public ResponseEntity<ApiResponse<VisitResponse>> editDates(Authentication authentication, @PathVariable UUID visitId,
                                                                   @Valid @RequestBody EditVisitDatesRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(visitService.editDates(userId, visitId, request)));
    }

    @DeleteMapping("/{visitId}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID visitId) {
        UUID userId = (UUID) authentication.getPrincipal();
        visitService.delete(userId, visitId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
