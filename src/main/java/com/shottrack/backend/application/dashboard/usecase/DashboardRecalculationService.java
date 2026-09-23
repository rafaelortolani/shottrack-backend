package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.gateway.DashboardSummaryGateway;
import com.shottrack.backend.application.dashboard.model.DashboardSummary;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.modality.model.ResultOrientation;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.Visit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final ResultTypeGateway resultTypeGateway;
    private final ModalityGateway modalityGateway;
    private final DashboardSummaryGateway dashboardSummaryGateway;

    public void recalculate(UUID userId) {
        DashboardStats stats = calculate(userId);

        DashboardSummary summary = dashboardSummaryGateway.findByUserId(userId)
                .orElseGet(() -> DashboardSummary.builder().userId(userId).build());
        summary.replaceWith(stats.trainingsThisMonth(), stats.shotsThisMonth(), stats.practicedModalities(),
                stats.highlightResultTypeName(), stats.highlightValue());

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
        List<String> practicedModalities = trainings.stream()
                .map(Training::getModalityId)
                .distinct()
                .map(modalityGateway::findById)
                .flatMap(Optional::stream)
                .map(Modality::getName)
                .sorted()
                .toList();

        Highlight highlight = calculateHighlight(series);

        return new DashboardStats(trainingsThisMonth, shotsThisMonth, practicedModalities,
                highlight == null ? null : highlight.resultTypeName(),
                highlight == null ? null : highlight.value());
    }

    /**
     * UC42/ADR-0011: entre os tipos MENOR_MELHOR/MAIOR_MELHOR, o que tem
     * mais registros preenchidos nas séries do atleta; valor é o melhor já
     * registrado nesse tipo, respeitando a orientação; desempate pelo mais
     * usado recentemente; ausente se nenhum tipo elegível tiver registro
     * válido ainda.
     */
    private Highlight calculateHighlight(List<Series> series) {
        Map<UUID, ResultType> eligibleTypesById = resultTypeGateway.findAll().stream()
                .filter(type -> type.getOrientation() != ResultOrientation.NAO_APLICAVEL)
                .collect(Collectors.toMap(ResultType::getId, Function.identity()));

        List<Entry> entries = series.stream()
                .flatMap(s -> seriesResultGateway.findAllBySeriesId(s.getId()).stream())
                .filter(result -> !result.isNotApplicable())
                .map(result -> toEntry(result.getResultTypeId(), result.getValue(), result.getCreatedAt(), eligibleTypesById))
                .filter(Objects::nonNull)
                .toList();

        return entries.stream()
                .collect(Collectors.groupingBy(entry -> entry.type().getId()))
                .values().stream()
                .map(this::toHighlight)
                .max(Comparator.comparingInt(Highlight::count).thenComparing(Highlight::mostRecentlyUsedAt))
                .orElse(null);
    }

    private Entry toEntry(UUID resultTypeId, String value, Instant recordedAt, Map<UUID, ResultType> eligibleTypesById) {
        ResultType type = eligibleTypesById.get(resultTypeId);
        if (type == null) {
            return null;
        }

        BigDecimal numericValue = parseNumericOrNull(value);
        if (numericValue == null) {
            return null;
        }

        return new Entry(type, numericValue, recordedAt);
    }

    private Highlight toHighlight(List<Entry> entriesForType) {
        ResultType type = entriesForType.get(0).type();
        Comparator<BigDecimal> bestValueOrder = type.getOrientation() == ResultOrientation.MENOR_MELHOR
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();

        BigDecimal bestValue = entriesForType.stream().map(Entry::value).min(bestValueOrder).orElseThrow();
        Instant mostRecentlyUsedAt = entriesForType.stream().map(Entry::recordedAt).max(Comparator.naturalOrder()).orElseThrow();

        return new Highlight(type.getName(), bestValue, entriesForType.size(), mostRecentlyUsedAt);
    }

    /**
     * ADR-0013: valor de resultado é texto — não numérico pro tipo em
     * questão é ignorado silenciosamente no cálculo (UC42, observação).
     */
    private BigDecimal parseNumericOrNull(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isInMonth(Instant instant, YearMonth month) {
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return YearMonth.from(date).equals(month);
    }

    private record Entry(ResultType type, BigDecimal value, Instant recordedAt) {
    }

    private record Highlight(String resultTypeName, BigDecimal value, int count, Instant mostRecentlyUsedAt) {
    }
}
