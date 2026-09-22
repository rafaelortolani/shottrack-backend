package com.shottrack.backend.application.series.usecase;

import com.shottrack.backend.application.ammunition.usecase.AmmunitionService;
import com.shottrack.backend.application.series.dto.RegisterSeriesRequest;
import com.shottrack.backend.application.series.dto.SeriesResponse;
import com.shottrack.backend.application.series.dto.SeriesResultResponse;
import com.shottrack.backend.application.series.dto.UpdateSeriesRequest;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.mapper.SeriesMapper;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.usecase.TrainingService;
import com.shottrack.backend.application.weapon.usecase.WeaponService;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesGateway seriesGateway;
    private final TrainingService trainingService;
    private final WeaponService weaponService;
    private final AmmunitionService ammunitionService;
    private final SeriesResultService seriesResultService;
    private final SeriesMapper seriesMapper;

    public SeriesResponse register(UUID userId, RegisterSeriesRequest request) {
        Training training = trainingService.findOwnedTrainingOrThrow(userId, request.trainingId());

        if (!training.isOpen()) {
            throw new BusinessException("TRAINING_ALREADY_CLOSED", HttpStatus.CONFLICT);
        }
        if (request.weaponId() != null) {
            weaponService.findOwnedWeaponOrThrow(userId, request.weaponId());
        }
        if (request.ammunitionId() != null) {
            ammunitionService.findOwnedAmmunitionOrThrow(userId, request.ammunitionId());
        }

        Series series = Series.builder()
                .trainingId(training.getId())
                .weaponId(request.weaponId())
                .ammunitionId(request.ammunitionId())
                .distanceMeters(request.distanceMeters())
                .target(request.target())
                .shotCount(request.shotCount())
                .notes(request.notes())
                .build();

        return toResponse(seriesGateway.save(series));
    }

    public List<SeriesResponse> listByTraining(UUID userId, UUID trainingId) {
        trainingService.findOwnedTrainingOrThrow(userId, trainingId);

        return seriesGateway.findAllByTrainingId(trainingId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * UC38/ADR-0013: edição parcial — só os campos enviados (não nulos) são
     * atualizados, mesmo padrão de Munição (UC15). É assim que "completar
     * depois" funciona: cada campo pode chegar em edições separadas.
     */
    public SeriesResponse update(UUID userId, UUID seriesId, UpdateSeriesRequest request) {
        Series series = findOwnedSeriesOrThrow(userId, seriesId);

        if (request.weaponId() != null) {
            weaponService.findOwnedWeaponOrThrow(userId, request.weaponId());
            series.setWeaponId(request.weaponId());
        }
        if (request.ammunitionId() != null) {
            ammunitionService.findOwnedAmmunitionOrThrow(userId, request.ammunitionId());
            series.setAmmunitionId(request.ammunitionId());
        }
        if (request.distanceMeters() != null) {
            series.setDistanceMeters(request.distanceMeters());
        }
        if (request.target() != null) {
            series.setTarget(request.target());
        }
        if (request.shotCount() != null) {
            BigDecimal registered = seriesResultService.sumShotRelatedResults(series.getId());
            if (registered.compareTo(BigDecimal.valueOf(request.shotCount())) > 0) {
                throw new BusinessException("SHOT_COUNT_LESS_THAN_REGISTERED_RESULTS", HttpStatus.CONFLICT);
            }
            series.setShotCountManually(request.shotCount());
        }
        if (request.notes() != null) {
            series.setNotes(request.notes());
        }

        return toResponse(seriesGateway.save(series));
    }

    /**
     * UC41/ADR-0013: sem bloqueio de exclusão — nada ainda referencia uma
     * série como "em uso". Os resultados somem junto via ON DELETE CASCADE
     * na constraint (series_results.series_id), não explicitamente aqui.
     */
    public void delete(UUID userId, UUID seriesId) {
        seriesGateway.delete(findOwnedSeriesOrThrow(userId, seriesId));
    }

    private Series findOwnedSeriesOrThrow(UUID userId, UUID seriesId) {
        Series series = seriesGateway.findById(seriesId)
                .orElseThrow(() -> new BusinessException("SERIES_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!trainingService.isOwnedByUser(series.getTrainingId(), userId)) {
            throw new BusinessException("SERIES_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        return series;
    }

    private SeriesResponse toResponse(Series series) {
        List<SeriesResultResponse> results = seriesResultService.listBySeriesId(series.getId());
        return seriesMapper.toResponse(series, results);
    }
}
