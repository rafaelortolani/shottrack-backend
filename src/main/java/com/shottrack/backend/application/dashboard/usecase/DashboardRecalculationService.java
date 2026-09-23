package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.gateway.DashboardSummaryGateway;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import com.shottrack.backend.application.dashboard.model.ModalityStats;
import com.shottrack.backend.application.dashboard.model.ResultRecord;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.series.model.SeriesResult;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.Visit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ADR-0015: lógica de agregação do dashboard, reaproveitada em dois
 * caminhos — o consumidor da fila (recalculate, grava em
 * dashboard_summary) e o fallback do UC42 (calculate, sem gravar, só
 * quando ainda não existe resumo pra esse atleta). Sempre recalcula do
 * zero a partir de Visita/Treino/Série/Resultado — nenhum deles guarda
 * referência direta ao atleta além de Visita, então a navegação é sempre
 * VisitGateway.findAllByUserId pra baixo, mesmo estilo de composição em
 * memória já usado por VisitService/TrainingService.
 */
@Service
@RequiredArgsConstructor
public class DashboardRecalculationService {

    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final SeriesGateway seriesGateway;
    private final SeriesResultGateway seriesResultGateway;
    private final ModalityGateway modalityGateway;
    private final DashboardSummaryGateway dashboardSummaryGateway;
    private final HighlightCalculator highlightCalculator;

    public void recalculate(UUID userId) {
        DashboardStats stats = calculate(userId);

        DashboardSummary summary = dashboardSummaryGateway.findByUserId(userId)
                .orElseGet(() -> DashboardSummary.builder().userId(userId).build());
        summary.replaceWith(stats.trainingsThisMonth(), stats.shotsThisMonth(), stats.practicedModalities(),
                stats.modalityStats(), stats.records());

        dashboardSummaryGateway.save(summary);
    }

    public DashboardStats calculate(UUID userId) {
        List<Visit> visits = visitGateway.findAllByUserId(userId);
        List<Training> trainings = visits.stream()
                .flatMap(visit -> trainingGateway.findAllByVisitId(visit.getId()).stream())
                .toList();
        List<Series> series = trainings.stream()
                .flatMap(training -> seriesGateway.findAllByTrainingId(training.getId()).stream())
                .toList();
        Map<UUID, List<SeriesResult>> resultsByTrainingId = series.stream()
                .collect(Collectors.groupingBy(Series::getTrainingId,
                        Collectors.flatMapping(s -> seriesResultGateway.findAllBySeriesId(s.getId()).stream(),
                                Collectors.toList())));

        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);

        int trainingsThisMonth = (int) trainings.stream()
                .filter(training -> isInMonth(training.getCreatedAt(), currentMonth))
                .count();
        int shotsThisMonth = series.stream()
                .filter(s -> isInMonth(s.getCreatedAt(), currentMonth))
                .map(Series::getShotCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        List<ModalityStats> modalityStats = calculateModalityStats(trainings, resultsByTrainingId);
        List<String> practicedModalities = modalityStats.stream()
                .map(ModalityStats::getModalityName)
                .toList();

        List<ResultRecord> records = highlightCalculator.records(
                        resultsByTrainingId.values().stream().flatMap(List::stream).toList()).stream()
                .map(best -> ResultRecord.builder().resultTypeName(best.resultTypeName()).bestValue(best.value()).build())
                .toList();

        return new DashboardStats(trainingsThisMonth, shotsThisMonth, practicedModalities, modalityStats, records);
    }

    /**
     * UC42 (Onda 1): uma linha por modalidade em que o atleta já treinou —
     * contagem de treinos e o destaque escopado às séries dela.
     */
    private List<ModalityStats> calculateModalityStats(List<Training> trainings,
                                                       Map<UUID, List<SeriesResult>> resultsByTrainingId) {
        Map<UUID, List<Training>> trainingsByModalityId = trainings.stream()
                .collect(Collectors.groupingBy(Training::getModalityId));
        Map<UUID, Modality> modalitiesById = trainingsByModalityId.keySet().stream()
                .map(modalityGateway::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(Modality::getId, Function.identity()));

        return trainingsByModalityId.entrySet().stream()
                .filter(entry -> modalitiesById.containsKey(entry.getKey()))
                .map(entry -> {
                    List<SeriesResult> results = entry.getValue().stream()
                            .flatMap(training -> resultsByTrainingId.getOrDefault(training.getId(), List.of()).stream())
                            .toList();
                    Optional<ResultHighlight> best = highlightCalculator.calculate(results);
                    return ModalityStats.builder()
                            .modalityName(modalitiesById.get(entry.getKey()).getName())
                            .trainingCount(entry.getValue().size())
                            .bestResultTypeName(best.map(ResultHighlight::resultTypeName).orElse(null))
                            .bestValue(best.map(ResultHighlight::value).orElse(null))
                            .build();
                })
                .sorted(Comparator.comparing(ModalityStats::getModalityName))
                .toList();
    }

    private boolean isInMonth(Instant instant, YearMonth month) {
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return YearMonth.from(date).equals(month);
    }
}
