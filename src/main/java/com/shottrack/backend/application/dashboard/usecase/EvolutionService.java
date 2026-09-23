package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.dto.EvolutionPointResponse;
import com.shottrack.backend.application.dashboard.dto.EvolutionRequest;
import com.shottrack.backend.application.dashboard.model.EvolutionMode;
import com.shottrack.backend.application.dashboard.model.EvolutionPeriod;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSelectionGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.model.ResultOrientation;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UC46/ADR-0016: evolução calculada sob demanda — não usa
 * dashboard_summary. Um ponto por dia com registro; dia sem registro não
 * gera ponto (nunca zero). O dia de um valor é o dia do treino (início,
 * em UTC — mesmo critério dos indicadores do mês no UC42), não o do
 * registro: completar o resultado depois não move o ponto de dia.
 */
@Service
@RequiredArgsConstructor
public class EvolutionService {

    private final PracticedModalityGateway practicedModalityGateway;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final ResultTypeGateway resultTypeGateway;
    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final SeriesGateway seriesGateway;
    private final SeriesResultGateway seriesResultGateway;

    public List<EvolutionPointResponse> getEvolution(UUID userId, EvolutionRequest request) {
        if (!practicedModalityGateway.existsByUserIdAndModalityId(userId, request.modalityId())) {
            throw new BusinessException("MODALITY_NOT_PRACTICED", HttpStatus.NOT_FOUND);
        }
        if (!modalityResultTypeSelectionGateway.existsByUserIdAndModalityIdAndResultTypeId(
                userId, request.modalityId(), request.resultTypeId())) {
            throw new BusinessException("RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING", HttpStatus.NOT_FOUND);
        }

        ResultType resultType = resultTypeGateway.findById(request.resultTypeId()).orElseThrow();
        EvolutionMode mode = EvolutionMode.valueOf(request.mode());
        // MELHOR depende de orientação (ADR-0011) — tipo sem orientação não
        // tem "melhor valor do dia", então não há ponto a mostrar
        if (mode == EvolutionMode.MELHOR && resultType.getOrientation() == ResultOrientation.NAO_APLICAVEL) {
            return List.of();
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = EvolutionPeriod.fromCode(request.period()).firstDay(today);

        Map<LocalDate, List<BigDecimal>> valuesByDay = visitGateway.findAllByUserId(userId).stream()
                .flatMap(visit -> trainingGateway.findAllByVisitId(visit.getId()).stream())
                .filter(training -> training.getModalityId().equals(request.modalityId()))
                .filter(training -> !dayOf(training).isBefore(firstDay) && !dayOf(training).isAfter(today))
                .flatMap(training -> seriesGateway.findAllByTrainingId(training.getId()).stream()
                        .flatMap(series -> seriesResultGateway
                                .findBySeriesIdAndResultTypeId(series.getId(), request.resultTypeId()).stream())
                        .filter(result -> !result.isNotApplicable())
                        .map(result -> ResultValues.parseNumeric(result.getValue()))
                        .flatMap(Optional::stream)
                        .map(value -> Map.entry(dayOf(training), value)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, TreeMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return valuesByDay.entrySet().stream()
                .map(day -> new EvolutionPointResponse(day.getKey(), aggregate(mode, resultType, day.getValue())))
                .toList();
    }

    private BigDecimal aggregate(EvolutionMode mode, ResultType resultType, List<BigDecimal> values) {
        if (mode == EvolutionMode.MELHOR) {
            return ResultValues.best(resultType.getOrientation(), values);
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
    }

    private LocalDate dayOf(Training training) {
        return training.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
