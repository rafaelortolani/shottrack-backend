package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.dto.ActiveVisitResponse;
import com.shottrack.backend.application.dashboard.dto.DashboardHighlightResponse;
import com.shottrack.backend.application.dashboard.dto.DashboardResponse;
import com.shottrack.backend.application.dashboard.dto.IndicatorResponse;
import com.shottrack.backend.application.dashboard.dto.MainActionResponse;
import com.shottrack.backend.application.dashboard.dto.ModalitySummaryResponse;
import com.shottrack.backend.application.dashboard.dto.OnboardingResponse;
import com.shottrack.backend.application.dashboard.dto.RecentTrainingResponse;
import com.shottrack.backend.application.dashboard.dto.ResultRecordResponse;
import com.shottrack.backend.application.dashboard.dto.SuggestedEvolutionResponse;
import com.shottrack.backend.application.dashboard.gateway.DashboardSummaryGateway;
import com.shottrack.backend.application.dashboard.model.MainActionType;
import com.shottrack.backend.application.dashboard.model.ModalityRecord;
import com.shottrack.backend.application.dashboard.model.ModalityStats;
import com.shottrack.backend.application.dashboard.model.OnboardingStep;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSelectionGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.series.model.SeriesResult;
import com.shottrack.backend.application.traininglocation.gateway.TrainingLocationGateway;
import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.TrainingStatus;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.weapon.gateway.WeaponGateway;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.application.weapon.model.Weapon;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC42 v7/ADR-0015/ADR-0022: recordes e estatísticas por modalidade vêm do
 * resumo pré-calculado em dashboard_summary; se ainda não existir linha pra
 * esse atleta (primeiro acesso, antes de qualquer evento ter sido processado
 * pela fila), calcula na hora como fallback — mesma lógica do consumidor
 * (DashboardRecalculationService.calculate), só que sem gravar. O filtro de
 * modalidade só seleciona dentro desses dados, nos dois caminhos.
 * Indicadores do mês dependem da data de hoje (janela móvel), então são sempre
 * contagens sob demanda. Onboarding, ação principal e últimos treinos também
 * ficam fora do resumo: dependem de escritas que não publicam evento de
 * recálculo e são baratos — no máximo 3 treinos, nunca o histórico inteiro.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_TRAININGS_LIMIT = 3;

    private final DashboardSummaryGateway dashboardSummaryGateway;
    private final DashboardRecalculationService dashboardRecalculationService;
    private final HighlightCalculator highlightCalculator;
    private final UserGateway userGateway;
    private final PracticedModalityGateway practicedModalityGateway;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final WeaponGateway weaponGateway;
    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final SeriesGateway seriesGateway;
    private final SeriesResultGateway seriesResultGateway;
    private final TrainingLocationGateway trainingLocationGateway;
    private final ModalityGateway modalityGateway;

    public DashboardResponse getDashboard(UUID userId, UUID modalityId) {
        if (modalityId != null && !practicedModalityGateway.existsByUserIdAndModalityId(userId, modalityId)) {
            throw new BusinessException("MODALITY_NOT_PRACTICED", HttpStatus.NOT_FOUND);
        }

        DashboardStats stats = loadStats(userId);
        List<Weapon> weapons = weaponGateway.findAllByUserId(userId);
        List<Visit> visits = visitGateway.findAllByUserId(userId);
        MonthWindows windows = MonthWindows.of(LocalDate.now(ZoneOffset.UTC));

        return new DashboardResponse(
                onboarding(userId, weapons),
                mainAction(visits),
                new IndicatorResponse(
                        trainingGateway.countByUserId(userId, modalityId, windows.currentFrom(), windows.currentTo()),
                        trainingGateway.countByUserId(userId, modalityId, windows.previousFrom(), windows.previousTo())),
                new IndicatorResponse(
                        seriesGateway.sumShotCountByUserId(userId, modalityId, windows.currentFrom(), windows.currentTo()),
                        seriesGateway.sumShotCountByUserId(userId, modalityId, windows.previousFrom(), windows.previousTo())),
                // ignora o filtro: "quantas modalidades o atleta pratica" não depende da modalidade olhada
                new IndicatorResponse(
                        trainingGateway.countDistinctModalitiesByUserId(userId, windows.currentFrom(), windows.currentTo()),
                        trainingGateway.countDistinctModalitiesByUserId(userId, windows.previousFrom(), windows.previousTo())),
                recentTrainings(visits, modalityId),
                records(stats, modalityId),
                suggestedEvolution(userId, stats, modalityId));
    }

    /**
     * UC42 v7 (aba Modalidades): sem filtro, lê as estatísticas por modalidade.
     */
    public List<ModalitySummaryResponse> getModalities(UUID userId) {
        return loadStats(userId).modalityStats().stream().map(this::toModalitySummaryResponse).toList();
    }

    private DashboardStats loadStats(UUID userId) {
        return dashboardSummaryGateway.findByUserId(userId)
                .map(summary -> new DashboardStats(summary.getModalityStats(), summary.getModalityRecords(),
                        summary.getRecords()))
                .orElseGet(() -> dashboardRecalculationService.calculate(userId));
    }

    /**
     * Sem filtro: recordes do atleta inteiro (ADR-0016); com filtro: os da modalidade.
     */
    private List<ResultRecordResponse> records(DashboardStats stats, UUID modalityId) {
        if (modalityId == null) {
            return stats.records().stream()
                    .map(record -> new ResultRecordResponse(record.getResultTypeName(), record.getBestValue()))
                    .toList();
        }
        return stats.modalityRecords().stream()
                .filter(record -> record.getModalityId().equals(modalityId))
                .sorted(Comparator.comparing(ModalityRecord::getResultTypeName))
                .map(record -> new ResultRecordResponse(record.getResultTypeName(), record.getBestValue()))
                .toList();
    }

    /**
     * UC42 v7: tipo de resultado mais registrado no filtro atual (desempate pelo
     * nome, pra resposta estável). Só sugere tipo ainda configurado na modalidade
     * (UC30) — o UC46 rejeita os demais.
     */
    private SuggestedEvolutionResponse suggestedEvolution(UUID userId, DashboardStats stats, UUID modalityId) {
        return stats.modalityRecords().stream()
                .filter(record -> modalityId == null || record.getModalityId().equals(modalityId))
                .sorted(Comparator.comparingInt(ModalityRecord::getRecordCount).reversed()
                        .thenComparing(ModalityRecord::getResultTypeName)
                        .thenComparing(ModalityRecord::getModalityId))
                .filter(record -> modalityResultTypeSelectionGateway.existsByUserIdAndModalityIdAndResultTypeId(
                        userId, record.getModalityId(), record.getResultTypeId()))
                .findFirst()
                .map(record -> new SuggestedEvolutionResponse(record.getModalityId(), record.getResultTypeId()))
                .orElse(null);
    }

    /**
     * UC42: nulo (ausente na resposta) quando não há nenhuma pendência.
     */
    private OnboardingResponse onboarding(UUID userId, List<Weapon> weapons) {
        List<OnboardingStep> pendingSteps = new ArrayList<>();
        if (!userGateway.findById(userId).orElseThrow().isProfileCompleted()) {
            pendingSteps.add(OnboardingStep.CREATE_PROFILE);
        }
        if (practicedModalityGateway.findAllByUserId(userId).isEmpty()) {
            pendingSteps.add(OnboardingStep.CONFIGURE_MODALITIES);
        }
        if (weapons.isEmpty()) {
            pendingSteps.add(OnboardingStep.REGISTER_WEAPON);
        }
        return pendingSteps.isEmpty() ? null : new OnboardingResponse(pendingSteps);
    }

    /**
     * UC42: nada impede mais de uma visita EM_ANDAMENTO ao mesmo tempo
     * (UC31) — a ação principal aponta pra mais recente delas. Uma visita
     * pode ter vários treinos simultâneos (ADR-0012), daí a lista.
     */
    private MainActionResponse mainAction(List<Visit> visits) {
        return visits.stream()
                .filter(Visit::isOpen)
                .max(Comparator.comparing(Visit::getStartedAt))
                .map(visit -> new MainActionResponse(MainActionType.CONTINUE_VISIT, toActiveVisitResponse(visit)))
                .orElseGet(() -> new MainActionResponse(MainActionType.START_VISIT, null));
    }

    private ActiveVisitResponse toActiveVisitResponse(Visit visit) {
        List<String> activeTrainingModalityNames = trainingGateway
                .findAllByVisitIdAndStatus(visit.getId(), TrainingStatus.IN_PROGRESS).stream()
                .map(training -> modalityName(training.getModalityId()))
                .toList();

        return new ActiveVisitResponse(visit.getId(), trainingLocationName(visit.getTrainingLocationId()),
                visit.getStartedAt(), activeTrainingModalityNames);
    }

    /**
     * UC42 v7: os 3 treinos mais recentes (da modalidade filtrada, se houver),
     * com destaque escopado às séries de cada treino (mesma regra do destaque
     * geral, HighlightCalculator).
     */
    private List<RecentTrainingResponse> recentTrainings(List<Visit> visits, UUID modalityId) {
        Map<UUID, Visit> visitsById = visits.stream().collect(Collectors.toMap(Visit::getId, Function.identity()));

        return visits.stream()
                .flatMap(visit -> trainingGateway.findAllByVisitId(visit.getId()).stream())
                .filter(training -> modalityId == null || training.getModalityId().equals(modalityId))
                // ADR-0024: "mais recentes" pela data da visita; o lançamento só desempata
                .sorted(Comparator.comparing((Training training) -> visitsById.get(training.getVisitId()).getStartedAt())
                        .thenComparing(Training::getCreatedAt).reversed())
                .limit(RECENT_TRAININGS_LIMIT)
                .map(training -> toRecentTrainingResponse(training, visitsById.get(training.getVisitId())))
                .toList();
    }

    private RecentTrainingResponse toRecentTrainingResponse(Training training, Visit visit) {
        List<SeriesResult> results = seriesGateway.findAllByTrainingId(training.getId()).stream()
                .flatMap(series -> seriesResultGateway.findAllBySeriesId(series.getId()).stream())
                .toList();
        DashboardHighlightResponse highlight = highlightCalculator.calculate(results)
                .map(best -> new DashboardHighlightResponse(best.resultTypeName(), best.value()))
                .orElse(null);

        return new RecentTrainingResponse(training.getId(), visit.getStartedAt(),
                trainingLocationName(visit.getTrainingLocationId()), modalityName(training.getModalityId()), highlight);
    }

    private ModalitySummaryResponse toModalitySummaryResponse(ModalityStats stats) {
        return new ModalitySummaryResponse(stats.getModalityId(), stats.getModalityName(), stats.getTrainingCount(),
                toHighlightResponse(stats.getBestResultTypeName(), stats.getBestValue()));
    }

    private DashboardHighlightResponse toHighlightResponse(String resultTypeName, BigDecimal value) {
        return resultTypeName == null ? null : new DashboardHighlightResponse(resultTypeName, value);
    }

    private String trainingLocationName(UUID trainingLocationId) {
        return trainingLocationGateway.findById(trainingLocationId)
                .map(TrainingLocation::getName)
                .orElse(null);
    }

    private String modalityName(UUID modalityId) {
        return modalityGateway.findById(modalityId)
                .map(Modality::getName)
                .orElse(null);
    }
}
