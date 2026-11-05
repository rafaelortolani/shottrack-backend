package com.shottrack.backend.application.plan;

import com.shottrack.backend.application.plan.dto.AdminUserResponse;
import com.shottrack.backend.application.plan.dto.AssignPlanRequest;
import com.shottrack.backend.application.plan.usecase.UserPlanAdminService;
import com.shottrack.backend.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** UC65 (ADR-0027): localizar atleta por e-mail e trocar o plano. Sob /api/admin/**, restrito a ADMIN (ADR-0021). */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserPlanAdminController {

    private final UserPlanAdminService service;

    @Operation(summary = "Buscar atleta por trecho do e-mail (UC65)",
            description = "Somente leitura; mínimo de 3 caracteres, até 10 resultados. Erros: VALIDATION_ERROR (400), UNAUTHORIZED (401), FORBIDDEN (403).")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> search(@RequestParam(required = false) String email) {
        return ResponseEntity.ok(ApiResponse.ok(service.searchByEmail(email)));
    }

    @Operation(summary = "Alterar o plano do atleta (UC65)",
            description = "Erros: USER_NOT_FOUND, PLAN_NOT_FOUND (404), PLAN_NOT_ACTIVE (409), VALIDATION_ERROR (400), UNAUTHORIZED (401), FORBIDDEN (403).")
    @PutMapping("/{userId}/plan")
    public ResponseEntity<ApiResponse<AdminUserResponse>> assign(Authentication authentication, @PathVariable UUID userId,
                                                                  @Valid @RequestBody AssignPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.assign((UUID) authentication.getPrincipal(), userId, request.planCode())));
    }
}
