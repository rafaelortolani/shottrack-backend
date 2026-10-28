package com.shottrack.backend.application.habituality;

import com.shottrack.backend.application.habituality.dto.HabitualityRuleRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityRuleResponse;
import com.shottrack.backend.application.habituality.dto.HabitualityVersionRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityVersionResponse;
import com.shottrack.backend.application.habituality.usecase.HabitualityRuleAdminService;
import com.shottrack.backend.application.habituality.usecase.HabitualityVersionAdminService;
import com.shottrack.backend.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * UC54/UC55 (ADR-0023): gestão administrativa. Tudo sob /api/admin/**, que o
 * AdminAuthorizationManager restringe a ADMIN (ADR-0021).
 */
@RestController
@RequestMapping("/api/admin/habituality")
@RequiredArgsConstructor
public class HabitualityAdminController {

    private final HabitualityRuleAdminService ruleService;
    private final HabitualityVersionAdminService versionService;

    @Operation(summary = "Listar regras de habitualidade (UC54)",
            description = "Erros: UNAUTHORIZED (401), FORBIDDEN (403).")
    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<HabitualityRuleResponse>>> listRules() {
        return ResponseEntity.ok(ApiResponse.ok(ruleService.list()));
    }

    @Operation(summary = "Criar regra (UC54)",
            description = "active ausente = true. Erros: VALIDATION_ERROR (400; nome vazio ou duplicado), "
                    + "UNAUTHORIZED (401), FORBIDDEN (403).")
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<HabitualityRuleResponse>> createRule(@Valid @RequestBody HabitualityRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(ruleService.create(request)));
    }

    @Operation(summary = "Editar regra, inclusive ativar/desativar (UC54)",
            description = "Erros: HABITUALITY_RULE_NOT_FOUND (404), VALIDATION_ERROR (400), "
                    + "UNAUTHORIZED (401), FORBIDDEN (403).")
    @PutMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<HabitualityRuleResponse>> updateRule(@PathVariable UUID id,
                                                                            @Valid @RequestBody HabitualityRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleService.update(id, request)));
    }

    @Operation(summary = "Listar versões da regra (UC55)",
            description = "Ordenadas por validFrom; current marca a versão em vigor. "
                    + "Erros: HABITUALITY_RULE_NOT_FOUND (404), UNAUTHORIZED (401), FORBIDDEN (403).")
    @GetMapping("/rules/{id}/versions")
    public ResponseEntity<ApiResponse<List<HabitualityVersionResponse>>> listVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(versionService.list(id)));
    }

    @Operation(summary = "Criar versão com níveis e exigências (UC55)",
            description = "A primeira versão da regra pode ter data passada ou de hoje; as seguintes, só futura. "
                    + "Erros: HABITUALITY_RULE_NOT_FOUND (404), HABITUALITY_VERSION_DATE_INVALID (400 data não futura; "
                    + "409 data duplicada), VALIDATION_ERROR (400), UNAUTHORIZED (401), FORBIDDEN (403).")
    @PostMapping("/rules/{id}/versions")
    public ResponseEntity<ApiResponse<HabitualityVersionResponse>> createVersion(@PathVariable UUID id,
                                                                                  @Valid @RequestBody HabitualityVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(versionService.create(id, request)));
    }

    @Operation(summary = "Substituir versão, níveis e exigências (UC55)",
            description = "Versão futura sempre editável; vigente/passada só sem atleta acompanhando a regra. "
                    + "Erros: HABITUALITY_VERSION_NOT_FOUND (404), HABITUALITY_VERSION_NOT_EDITABLE (409), "
                    + "HABITUALITY_VERSION_DATE_INVALID, VALIDATION_ERROR (400), UNAUTHORIZED (401), FORBIDDEN (403).")
    @PutMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<HabitualityVersionResponse>> updateVersion(@PathVariable UUID versionId,
                                                                                  @Valid @RequestBody HabitualityVersionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(versionService.update(versionId, request)));
    }

    @Operation(summary = "Remover versão futura (UC55)",
            description = "Só versão com validFrom futuro. Erros: HABITUALITY_VERSION_NOT_FOUND (404), "
                    + "HABITUALITY_VERSION_NOT_EDITABLE (409), UNAUTHORIZED (401), FORBIDDEN (403).")
    @DeleteMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(@PathVariable UUID versionId) {
        versionService.delete(versionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
