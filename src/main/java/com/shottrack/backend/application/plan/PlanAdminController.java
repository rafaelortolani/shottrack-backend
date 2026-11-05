package com.shottrack.backend.application.plan;

import com.shottrack.backend.application.plan.dto.CreatePlanRequest;
import com.shottrack.backend.application.plan.dto.EditPlanLimitsRequest;
import com.shottrack.backend.application.plan.dto.PlanAuditResponse;
import com.shottrack.backend.application.plan.dto.PlanResponse;
import com.shottrack.backend.application.plan.dto.UpdatePlanRequest;
import com.shottrack.backend.application.plan.usecase.PlanAdminService;
import com.shottrack.backend.application.plan.usecase.PlanAuditQueryService;
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

/** UC63/UC64/UC67/UC68/UC69 (ADR-0027): sob /api/admin/**, restrito a ADMIN (ADR-0021). Não existe rota de exclusão de plano. */
@RestController
@RequestMapping("/api/admin/plans")
@RequiredArgsConstructor
public class PlanAdminController {

    private final PlanAdminService planAdminService;
    private final PlanAuditQueryService auditQueryService;

    @Operation(summary = "Listar planos e limites (UC63)", description = "Erros: UNAUTHORIZED (401), FORBIDDEN (403).")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(planAdminService.list()));
    }

    @Operation(summary = "Criar plano (UC68)",
            description = "Erros: PLAN_CODE_INVALID, PLAN_NAME_INVALID (400), PLAN_NOT_FOUND (404, copyFromCode), "
                    + "PLAN_CODE_ALREADY_EXISTS, PLAN_NAME_ALREADY_EXISTS, PLAN_COUNT_LIMIT_REACHED (409), UNAUTHORIZED (401), FORBIDDEN (403).")
    @PostMapping
    public ResponseEntity<ApiResponse<PlanResponse>> create(Authentication authentication, @RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(planAdminService.create(actor(authentication), request)));
    }

    @Operation(summary = "Editar plano: nome, ativo, padrão (UC69)",
            description = "Edição parcial. Erros: PLAN_NOT_FOUND (404), PLAN_NAME_INVALID, VALIDATION_ERROR (400), "
                    + "PLAN_NAME_ALREADY_EXISTS, PLAN_DEFAULT_CANNOT_BE_DEACTIVATED, PLAN_NOT_ACTIVE, PLAN_DEFAULT_REQUIRED (409), "
                    + "UNAUTHORIZED (401), FORBIDDEN (403).")
    @PatchMapping("/{code}")
    public ResponseEntity<ApiResponse<PlanResponse>> update(Authentication authentication, @PathVariable String code,
                                                             @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(planAdminService.update(actor(authentication), code, request)));
    }

    @Operation(summary = "Editar limites do plano (UC64)",
            description = "Edição parcial e atômica. Erros: PLAN_NOT_FOUND (404), PLAN_LIMIT_INVALID_KEY, PLAN_LIMIT_OUT_OF_RANGE, "
                    + "VALIDATION_ERROR (400), UNAUTHORIZED (401), FORBIDDEN (403).")
    @PatchMapping("/{code}/limits")
    public ResponseEntity<ApiResponse<PlanResponse>> editLimits(Authentication authentication, @PathVariable String code,
                                                                 @Valid @RequestBody EditPlanLimitsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(planAdminService.editLimits(actor(authentication), code, request)));
    }

    @Operation(summary = "Histórico de alterações de planos (UC67)",
            description = "limit 1 a 200 (padrão 50). Erros: VALIDATION_ERROR (400), UNAUTHORIZED (401), FORBIDDEN (403).")
    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<List<PlanAuditResponse>>> audit(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(auditQueryService.recent(limit)));
    }

    private static UUID actor(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
