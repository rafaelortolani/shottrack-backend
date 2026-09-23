package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.dto.ActiveVisitResponse;
import com.shottrack.backend.application.dashboard.dto.DashboardHighlightResponse;
import com.shottrack.backend.application.dashboard.dto.DashboardResponse;
import com.shottrack.backend.application.dashboard.dto.MainActionResponse;
import com.shottrack.backend.application.dashboard.dto.ModalitySummaryResponse;
import com.shottrack.backend.application.dashboard.dto.OnboardingResponse;
import com.shottrack.backend.application.dashboard.dto.RecentTrainingResponse;
import com.shottrack.backend.application.dashboard.dto.WeaponCollectionResponse;
import com.shottrack.backend.application.dashboard.gateway.DashboardSummaryGateway;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import com.shottrack.backend.application.dashboard.model.MainActionType;
import com.shottrack.backend.application.dashboard.model.ModalityStats;
import com.shottrack.backend.application.dashboard.model.OnboardingStep;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
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
import com.shottrack.backend.application.weapon.gateway.WeaponBrandGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponModelGateway;
import com.shottrack.backend.application.weapon.model.Weapon;
import com.shottrack.backend.application.weapon.model.WeaponBrand;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * UC42/ADR-0015: lê o resumo pré-calculado de dashboard_summary; se ainda
 * não existir linha pra esse atleta (primeiro acesso, antes de qualquer
 * evento ter sido processado pela fila), calcula na hora como fallback —
 * mesma lógica do consumidor (DashboardRecalculationService.calculate),
 * só que sem gravar.
 * Onboarding, ação principal, últimos treinos e acervo ficam sempre fora
 * do resumo e são calculados aqui, nos dois caminhos: dependem de escritas
 * que não publicam evento de recálculo (perfil, modalidades praticadas,
 * armas, iniciar visita/abrir treino, editar local), e são baratos — no
 * máximo 5 treinos, nunca o histórico inteiro.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_TRAININGS_LIMIT = 5;
    private static final int WEAPON_NAMES_LIMIT = 3;

    private final DashboardSummaryGateway dashboardSummaryGateway;
    private final DashboardRecalculationService dashboardRecalculationService;
    private final HighlightCalculator highlightCalculator;
    private final UserGateway userGateway;
    private final PracticedModalityGateway practicedModalityGateway;
    private final WeaponGateway weaponGateway;
    private final WeaponBrandGateway weaponBrandGateway;
    private final WeaponModelGateway weaponModelGateway;
    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final SeriesGateway seriesGateway;
    private final SeriesResultGateway seriesResultGateway;
    private final TrainingLocationGateway trainingLocationGateway;
    private final ModalityGateway modalityGateway;

    public DashboardResponse getDashboard(UUID userId) {
        DashboardStats stats = dashboardSummaryGateway.findByUserId(userId)
                .map(this::toStats)
                .orElseGet(() -> dashboardRecalculationService.calculate(userId));

        List<Weapon> weapons = weaponGateway.findAllByUserId(userId);
        List<Visit> visits = visitGateway.findAllByUserId(userId);

        return new DashboardResponse(
                onboarding(userId, weapons),
                mainAction(visits),
                stats.trainingsThisMonth(),
                stats.shotsThisMonth(),
                stats.practicedModalities(),
                recentTrainings(visits),
                stats.modalityStats().stream().map(this::toModalitySummaryResponse).toList(),
                weaponCollection(weapons),
                toHighlightResponse(stats.highlightResultTypeName(), stats.highlightValue()));
    }

    private DashboardStats toStats(DashboardSummary summary) {
        return new DashboardStats(summary.getTrainingsThisMonth(), summary.getShotsThisMonth(),
                summary.getPracticedModalities(), summary.getModalityStats(),
                summary.getHighlightResultTypeName(), summary.getHighlightValue());
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
                .max(Comparator.comparing(Visit::getCreatedAt))
                .map(visit -> new MainActionResponse(MainActionType.CONTINUE_VISIT, toActiveVisitResponse(visit)))
                .orElseGet(() -> new MainActionResponse(MainActionType.START_VISIT, null));
    }

    private ActiveVisitResponse toActiveVisitResponse(Visit visit) {
        List<String> activeTrainingModalityNames = trainingGateway
                .findAllByVisitIdAndStatus(visit.getId(), TrainingStatus.IN_PROGRESS).stream()
                .map(training -> modalityName(training.getModalityId()))
                .toList();

        return new ActiveVisitResponse(visit.getId(), trainingLocationName(visit.getTrainingLocationId()),
                visit.getCreatedAt(), activeTrainingModalityNames);
    }

    /**
     * UC42: destaque escopado às séries de cada treino (mesma regra do
     * destaque geral, HighlightCalculator).
     */
    private List<RecentTrainingResponse> recentTrainings(List<Visit> visits) {
        Map<UUID, Visit> visitsById = visits.stream().collect(Collectors.toMap(Visit::getId, Function.identity()));

        return visits.stream()
                .flatMap(visit -> trainingGateway.findAllByVisitId(visit.getId()).stream())
                .sorted(Comparator.comparing(Training::getCreatedAt).reversed())
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

        return new RecentTrainingResponse(training.getId(), training.getCreatedAt(),
                trainingLocationName(visit.getTrainingLocationId()), modalityName(training.getModalityId()), highlight);
    }

    private ModalitySummaryResponse toModalitySummaryResponse(ModalityStats stats) {
        return new ModalitySummaryResponse(stats.getModalityName(), stats.getTrainingCount(),
                toHighlightResponse(stats.getBestResultTypeName(), stats.getBestValue()));
    }

    /**
     * UC42: as 3 armas cadastradas mais recentemente. Nome é o apelido
     * (UC06) quando existe; senão, marca + modelo do catálogo.
     */
    private WeaponCollectionResponse weaponCollection(List<Weapon> weapons) {
        List<String> weaponNames = weapons.stream()
                .sorted(Comparator.comparing(Weapon::getCreatedAt).reversed())
                .limit(WEAPON_NAMES_LIMIT)
                .map(this::weaponName)
                .toList();

        return new WeaponCollectionResponse(weapons.size(), weaponNames);
    }

    private String weaponName(Weapon weapon) {
        if (weapon.getNickname() != null && !weapon.getNickname().isBlank()) {
            return weapon.getNickname();
        }
        return Stream.of(
                        weaponBrandGateway.findById(weapon.getBrandId()).map(WeaponBrand::getName),
                        weaponModelGateway.findById(weapon.getModelId()).map(WeaponModel::getName))
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" "));
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
