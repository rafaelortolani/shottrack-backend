package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.dashboard.dto.WeaponComparisonRequest;
import com.shottrack.backend.application.dashboard.dto.WeaponComparisonResponse;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSelectionGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.model.ResultOrientation;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.weapon.usecase.WeaponService;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UC48: comparação calculada sob demanda (mesmo raciocínio do ADR-0016) —
 * combinação livre de armas e tipo de resultado não compensa pré-cálculo.
 * O dia de uma série é o dia do treino (início, em UTC — mesmo critério da
 * evolução, UC46). A contagem é de séries com a arma, tenham ou não valor
 * do tipo escolhido; média e melhor usam só os valores numéricos.
 */
@Service
@RequiredArgsConstructor
public class WeaponComparisonService {

    private final PracticedModalityGateway practicedModalityGateway;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final ResultTypeGateway resultTypeGateway;
    private final WeaponService weaponService;
    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final SeriesGateway seriesGateway;
    private final SeriesResultGateway seriesResultGateway;

    public List<WeaponComparisonResponse> compare(UUID userId, WeaponComparisonRequest request) {
        if (!practicedModalityGateway.existsByUserIdAndModalityId(userId, request.modalityId())) {
            throw new BusinessException("MODALITY_NOT_PRACTICED", HttpStatus.NOT_FOUND);
        }
        if (!modalityResultTypeSelectionGateway.existsByUserIdAndModalityIdAndResultTypeId(
                userId, request.modalityId(), request.resultTypeId())) {
            throw new BusinessException("RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING", HttpStatus.NOT_FOUND);
        }
        List<UUID> weaponIds = request.weaponIds().stream().distinct().toList();
        weaponIds.forEach(weaponId -> weaponService.findOwnedWeaponOrThrow(userId, weaponId));

        ResultType resultType = resultTypeGateway.findById(request.resultTypeId()).orElseThrow();

        Map<UUID, List<Series>> seriesByWeapon = visitGateway.findAllByUserId(userId).stream()
                .flatMap(visit -> trainingGateway.findAllByVisitId(visit.getId()).stream())
                .filter(training -> training.getModalityId().equals(request.modalityId()))
                .filter(training -> isWithinPeriod(dayOf(training), request))
                .flatMap(training -> seriesGateway.findAllByTrainingId(training.getId()).stream())
                .filter(series -> series.getWeaponId() != null && weaponIds.contains(series.getWeaponId()))
                .collect(Collectors.groupingBy(Series::getWeaponId));

        return weaponIds.stream()
                .map(weaponId -> toResponse(weaponId, seriesByWeapon.getOrDefault(weaponId, List.of()), resultType))
                .toList();
    }

    private WeaponComparisonResponse toResponse(UUID weaponId, List<Series> series, ResultType resultType) {
        List<BigDecimal> values = series.stream()
                .flatMap(s -> seriesResultGateway.findBySeriesIdAndResultTypeId(s.getId(), resultType.getId()).stream())
                .filter(result -> !result.isNotApplicable())
                .map(result -> ResultValues.parseNumeric(result.getValue()))
                .flatMap(Optional::stream)
                .toList();

        BigDecimal average = values.isEmpty() ? null : values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
        BigDecimal best = values.isEmpty() || resultType.getOrientation() == ResultOrientation.NAO_APLICAVEL
                ? null
                : ResultValues.best(resultType.getOrientation(), values);

        return new WeaponComparisonResponse(weaponService.getResponseById(weaponId), series.size(), average, best);
    }

    private boolean isWithinPeriod(LocalDate day, WeaponComparisonRequest request) {
        return (request.startDate() == null || !day.isBefore(request.startDate()))
                && (request.endDate() == null || !day.isAfter(request.endDate()));
    }

    private LocalDate dayOf(Training training) {
        return training.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
