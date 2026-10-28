package com.shottrack.backend.application.dashboard;

import com.shottrack.backend.application.dashboard.dto.DashboardResponse;
import com.shottrack.backend.application.dashboard.dto.ModalitySummaryResponse;
import com.shottrack.backend.application.dashboard.dto.EvolutionRequest;
import com.shottrack.backend.application.dashboard.dto.WeaponComparisonRequest;
import com.shottrack.backend.application.dashboard.dto.WeaponComparisonResponse;
import com.shottrack.backend.application.dashboard.usecase.DashboardService;
import com.shottrack.backend.application.dashboard.usecase.EvolutionService;
import com.shottrack.backend.application.dashboard.usecase.WeaponComparisonService;
import com.shottrack.backend.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final EvolutionService evolutionService;
    private final WeaponComparisonService weaponComparisonService;

    @Operation(summary = "Resumo e recordes (UC42 v7)",
            description = "Indicadores do mês com comparação ao período anterior, últimos 3 treinos, recordes e evolução "
                    + "sugerida. `modalityId` (opcional) filtra tudo, exceto modalitiesPracticed. "
                    + "Erros: MODALITY_NOT_PRACTICED (404), UNAUTHORIZED (401).")
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> get(Authentication authentication,
                                                              @RequestParam(required = false) UUID modalityId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(userId, modalityId)));
    }

    @Operation(summary = "Modalidades praticadas (UC42 v7)",
            description = "Aba Modalidades: nome, nº de treinos e melhor valor do tipo mais registrado. Não aceita filtro.")
    @GetMapping("/modalities")
    public ResponseEntity<ApiResponse<List<ModalitySummaryResponse>>> modalities(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getModalities(userId)));
    }

    @Operation(summary = "Evolução (UC46)",
            description = "Sem comparePrevious (ou false): data = lista de pontos. Com comparePrevious=true: data = "
                    + "{points, previousPoints}, o período anterior de mesma duração, mesmo modo e mesmas regras.")
    @GetMapping("/evolution")
    public ResponseEntity<ApiResponse<Object>> evolution(Authentication authentication,
                                                                               @Valid @ModelAttribute EvolutionRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(evolutionService.getEvolution(userId, request)));
    }

    @GetMapping("/weapon-comparison")
    public ResponseEntity<ApiResponse<List<WeaponComparisonResponse>>> weaponComparison(Authentication authentication,
                                                                                        @Valid @ModelAttribute WeaponComparisonRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(weaponComparisonService.compare(userId, request)));
    }
}
