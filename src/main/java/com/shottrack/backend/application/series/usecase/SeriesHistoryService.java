package com.shottrack.backend.application.series.usecase;

import com.shottrack.backend.application.ammunition.dto.AmmunitionResponse;
import com.shottrack.backend.application.ammunition.usecase.AmmunitionService;
import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.usecase.ModalityCatalogService;
import com.shottrack.backend.application.series.dto.SeriesHistoryRequest;
import com.shottrack.backend.application.series.dto.SeriesHistoryResponse;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.mapper.SeriesMapper;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationResponse;
import com.shottrack.backend.application.traininglocation.usecase.TrainingLocationService;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import com.shottrack.backend.application.weapon.usecase.WeaponService;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC47: histórico calculado sob demanda (mesmo raciocínio do ADR-0016) —
 * filtros combináveis livremente não compensam pré-cálculo. O dia de uma
 * série é o dia do treino (início, em UTC — mesmo critério da evolução,
 * UC46). Arma, munição, local e modalidade são carregados uma vez por
 * consulta e indexados por id, em vez de uma busca por série.
 */
@Service
@RequiredArgsConstructor
public class SeriesHistoryService {

    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final SeriesGateway seriesGateway;
    private final SeriesResultService seriesResultService;
    private final PracticedModalityGateway practicedModalityGateway;
    private final WeaponService weaponService;
    private final AmmunitionService ammunitionService;
    private final TrainingLocationService trainingLocationService;
    private final ModalityCatalogService modalityCatalogService;
    private final SeriesMapper seriesMapper;

    public List<SeriesHistoryResponse> getHistory(UUID userId, SeriesHistoryRequest request) {
        if (request.weaponId() != null) {
            weaponService.findOwnedWeaponOrThrow(userId, request.weaponId());
        }
        if (request.modalityId() != null
                && !practicedModalityGateway.existsByUserIdAndModalityId(userId, request.modalityId())) {
            throw new BusinessException("MODALITY_NOT_PRACTICED", HttpStatus.NOT_FOUND);
        }

        Map<UUID, WeaponResponse> weaponsById = indexById(weaponService.listByUser(userId), WeaponResponse::id);
        Map<UUID, AmmunitionResponse> ammunitionsById = indexById(ammunitionService.listByUser(userId), AmmunitionResponse::id);
        Map<UUID, TrainingLocationResponse> locationsById =
                indexById(trainingLocationService.listByUser(userId), TrainingLocationResponse::id);
        Map<UUID, ModalityResponse> modalitiesById = indexById(modalityCatalogService.listAll(), ModalityResponse::id);

        return visitGateway.findAllByUserId(userId).stream()
                .flatMap(visit -> trainingGateway.findAllByVisitId(visit.getId()).stream()
                        .filter(training -> matchesTraining(training, request))
                        .flatMap(training -> seriesGateway.findAllByTrainingId(training.getId()).stream()
                                .filter(series -> matchesSeries(series, request))
                                .map(series -> new Entry(visit, training, series))))
                .sorted(Comparator.comparing((Entry entry) -> entry.training().getCreatedAt())
                        .thenComparing(entry -> entry.series().getCreatedAt())
                        .reversed())
                .map(entry -> seriesMapper.toHistoryResponse(
                        entry.series(),
                        dayOf(entry.training()),
                        locationsById.get(entry.visit().getTrainingLocationId()),
                        modalitiesById.get(entry.training().getModalityId()),
                        entry.series().getWeaponId() == null ? null : weaponsById.get(entry.series().getWeaponId()),
                        entry.series().getAmmunitionId() == null ? null : ammunitionsById.get(entry.series().getAmmunitionId()),
                        seriesResultService.listBySeriesId(entry.series().getId())))
                .toList();
    }

    private boolean matchesTraining(Training training, SeriesHistoryRequest request) {
        LocalDate day = dayOf(training);
        return (request.modalityId() == null || training.getModalityId().equals(request.modalityId()))
                && (request.startDate() == null || !day.isBefore(request.startDate()))
                && (request.endDate() == null || !day.isAfter(request.endDate()));
    }

    /**
     * ADR-0013: arma e distância são opcionais na série — com o filtro
     * correspondente informado, série sem o campo preenchido não entra.
     */
    private boolean matchesSeries(Series series, SeriesHistoryRequest request) {
        BigDecimal distance = series.getDistanceMeters();
        return (request.weaponId() == null || request.weaponId().equals(series.getWeaponId()))
                && (request.minDistanceMeters() == null
                        || (distance != null && distance.compareTo(request.minDistanceMeters()) >= 0))
                && (request.maxDistanceMeters() == null
                        || (distance != null && distance.compareTo(request.maxDistanceMeters()) <= 0));
    }

    private LocalDate dayOf(Training training) {
        return training.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static <T> Map<UUID, T> indexById(List<T> items, Function<T, UUID> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private record Entry(Visit visit, Training training, Series series) {
    }
}
