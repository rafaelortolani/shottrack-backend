package com.shottrack.backend.application.habituality;

import com.shottrack.backend.application.habituality.dto.AthleteHabitualityRuleResponse;
import com.shottrack.backend.application.habituality.dto.HabitualityProgressResponse;
import com.shottrack.backend.application.habituality.dto.HabitualityTrackingRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityTrackingResponse;
import com.shottrack.backend.application.habituality.dto.PersonalGoalRequest;
import com.shottrack.backend.application.habituality.dto.PersonalGoalResponse;
import com.shottrack.backend.application.habituality.usecase.HabitualityProgressService;
import com.shottrack.backend.application.habituality.usecase.HabitualityTrackingService;
import com.shottrack.backend.application.habituality.usecase.PersonalGoalService;
import com.shottrack.backend.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * UC56/UC57/UC58 (ADR-0023): lado do atleta.
 */
@RestController
@RequestMapping("/api/habituality")
@RequiredArgsConstructor
public class HabitualityController {

    private final HabitualityTrackingService trackingService;
    private final HabitualityProgressService progressService;
    private final PersonalGoalService personalGoalService;

    @Operation(summary = "Regras ativas com a versão vigente (UC56)",
            description = "Níveis e exigências da versão em vigor e, se houver, upcomingVersion. "
                    + "Regra sem versão vigente não aparece. Erros: UNAUTHORIZED (401).")
    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<AthleteHabitualityRuleResponse>>> rules() {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.listRules()));
    }

    @Operation(summary = "Escolher ou trocar regra e nível acompanhados (UC56)",
            description = "Um acompanhamento por atleta; o nível é casado pelo nome. Erros: "
                    + "HABITUALITY_RULE_NOT_FOUND (404), HABITUALITY_RULE_INACTIVE (409), "
                    + "HABITUALITY_VERSION_NOT_FOUND (404), HABITUALITY_LEVEL_NOT_FOUND (404), UNAUTHORIZED (401).")
    @PutMapping("/tracking")
    public ResponseEntity<ApiResponse<HabitualityTrackingResponse>> track(Authentication authentication,
                                                                            @Valid @RequestBody HabitualityTrackingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.track(userId(authentication), request)));
    }

    @Operation(summary = "Parar de acompanhar (UC56)", description = "Idempotente. Erros: UNAUTHORIZED (401).")
    @DeleteMapping("/tracking")
    public ResponseEntity<ApiResponse<Void>> stopTracking(Authentication authentication) {
        trackingService.stopTracking(userId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "Progresso de habitualidade (UC57)",
            description = "Janela móvel de windowMonths meses até hoje; só visitas encerradas. Sem acompanhamento, "
                    + "rule é null; ruleInactive e levelRequired dispensam a apuração. Erros: UNAUTHORIZED (401).")
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<HabitualityProgressResponse>> progress(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(progressService.getProgress(userId(authentication))));
    }

    @Operation(summary = "Definir meta pessoal de treinos no ano (UC58)",
            description = "yearlyTrainings de 1 a 1000. Erros: VALIDATION_ERROR (400), UNAUTHORIZED (401).")
    @PutMapping("/personal-goal")
    public ResponseEntity<ApiResponse<PersonalGoalResponse>> setGoal(Authentication authentication,
                                                                       @Valid @RequestBody PersonalGoalRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(personalGoalService.set(userId(authentication), request)));
    }

    @Operation(summary = "Remover meta pessoal (UC58)", description = "Idempotente. Erros: UNAUTHORIZED (401).")
    @DeleteMapping("/personal-goal")
    public ResponseEntity<ApiResponse<Void>> removeGoal(Authentication authentication) {
        personalGoalService.remove(userId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private static UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
