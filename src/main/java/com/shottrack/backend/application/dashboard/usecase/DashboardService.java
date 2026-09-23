package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.dto.DashboardHighlightResponse;
import com.shottrack.backend.application.dashboard.dto.DashboardResponse;
import com.shottrack.backend.application.dashboard.dto.RecentVisitResponse;
import com.shottrack.backend.application.dashboard.gateway.DashboardSummaryGateway;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.traininglocation.gateway.TrainingLocationGateway;
import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.Visit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UC42/ADR-0015: lê o resumo pré-calculado de dashboard_summary; se ainda
 * não existir linha pra esse atleta (primeiro acesso, antes de qualquer
 * evento ter sido processado pela fila), calcula na hora como fallback —
 * mesma lógica do consumidor (DashboardRecalculationService.calculate),
 * só que sem gravar. "3 visitas mais recentes" fica sempre fora do
 * resumo pré-calculado (ADR-0015) — calculada direto aqui, os dois
 * caminhos de leitura.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_VISITS_LIMIT = 3;

    private final DashboardSummaryGateway dashboardSummaryGateway;
    private final DashboardRecalculationService dashboardRecalculationService;
    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final TrainingLocationGateway trainingLocationGateway;
    private final ModalityGateway modalityGateway;

    public DashboardResponse getDashboard(UUID userId) {
        DashboardStats stats = dashboardSummaryGateway.findByUserId(userId)
                .map(this::toStats)
                .orElseGet(() -> dashboardRecalculationService.calculate(userId));

        DashboardHighlightResponse highlight = stats.highlightResultTypeName() == null
                ? null
                : new DashboardHighlightResponse(stats.highlightResultTypeName(), stats.highlightValue());

        return new DashboardResponse(stats.trainingsThisMonth(), stats.shotsThisMonth(),
                stats.practicedModalities(), recentVisits(userId), highlight);
    }

    private DashboardStats toStats(DashboardSummary summary) {
        return new DashboardStats(summary.getTrainingsThisMonth(), summary.getShotsThisMonth(),
                summary.getPracticedModalities(), summary.getHighlightResultTypeName(), summary.getHighlightValue());
    }

    private List<RecentVisitResponse> recentVisits(UUID userId) {
        return visitGateway.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(Visit::getCreatedAt).reversed())
                .limit(RECENT_VISITS_LIMIT)
                .map(this::toRecentVisitResponse)
                .toList();
    }

    private RecentVisitResponse toRecentVisitResponse(Visit visit) {
        String trainingLocationName = trainingLocationGateway.findById(visit.getTrainingLocationId())
                .map(TrainingLocation::getName)
                .orElse(null);

        List<String> modalityNames = trainingGateway.findAllByVisitId(visit.getId()).stream()
                .map(Training::getModalityId)
                .distinct()
                .map(modalityGateway::findById)
                .flatMap(Optional::stream)
                .map(Modality::getName)
                .toList();

        return new RecentVisitResponse(visit.getId(), trainingLocationName, visit.getCreatedAt(), modalityNames);
    }
}
