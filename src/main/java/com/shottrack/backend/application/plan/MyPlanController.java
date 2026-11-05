package com.shottrack.backend.application.plan;

import com.shottrack.backend.application.plan.dto.MyPlanResponse;
import com.shottrack.backend.application.plan.usecase.MyPlanService;
import com.shottrack.backend.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** UC66 (ADR-0027): o atleta vê o próprio plano, os limites efetivos e o uso. */
@RestController
@RequestMapping("/api/users/me/plan")
@RequiredArgsConstructor
public class MyPlanController {

    private final MyPlanService myPlanService;

    @Operation(summary = "Consultar meu plano e uso (UC66)", description = "Erros: UNAUTHORIZED (401).")
    @GetMapping
    public ResponseEntity<ApiResponse<MyPlanResponse>> get(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(myPlanService.get((UUID) authentication.getPrincipal())));
    }
}
