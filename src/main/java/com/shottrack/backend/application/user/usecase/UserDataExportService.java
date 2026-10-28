package com.shottrack.backend.application.user.usecase;

import com.shottrack.backend.application.accessory.gateway.AccessoryGateway;
import com.shottrack.backend.application.accessory.gateway.AccessoryTypeGateway;
import com.shottrack.backend.application.accessory.gateway.AccessoryWeaponGateway;
import com.shottrack.backend.application.accessory.model.Accessory;
import com.shottrack.backend.application.accessory.model.AccessoryType;
import com.shottrack.backend.application.accessory.model.AccessoryWeapon;
import com.shottrack.backend.application.ammunition.gateway.AmmunitionGateway;
import com.shottrack.backend.application.ammunition.gateway.AmmunitionManufacturerGateway;
import com.shottrack.backend.application.ammunition.model.Ammunition;
import com.shottrack.backend.application.ammunition.model.AmmunitionManufacturer;
import com.shottrack.backend.application.habituality.gateway.AthleteHabitualityGateway;
import com.shottrack.backend.application.habituality.gateway.HabitualityRuleGateway;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSelectionGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.modality.model.ModalityResultTypeSelection;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.series.model.SeriesResult;
import com.shottrack.backend.application.traininglocation.gateway.TrainingLocationGateway;
import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import com.shottrack.backend.application.user.dto.UserDataExportResponse;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.AccessoryExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.AmmunitionExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.HabitualityExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.PracticedModalityExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.TrackingExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.ResultExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.SeriesExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.TrainingExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.VisitExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.WeaponExport;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.mapper.UserDataExportMapper;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.weapon.gateway.WeaponBrandGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponCaliberGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponModelGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponTypeGateway;
import com.shottrack.backend.application.weapon.model.Weapon;
import com.shottrack.backend.application.weapon.model.WeaponBrand;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import com.shottrack.backend.application.weapon.model.WeaponType;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC53/ADR-0019: reúne tudo o que o sistema guarda sobre o atleta. Sem N+1:
 * cada nível (visitas → treinos → séries → resultados) é buscado de uma vez
 * pelos ids do nível acima, e os catálogos pequenos (tipos, marcas, calibres,
 * modalidades...) são carregados uma vez e indexados por id.
 */
@Service
@RequiredArgsConstructor
public class UserDataExportService {

    private final UserGateway userGateway;
    private final PracticedModalityGateway practicedModalityGateway;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final ModalityGateway modalityGateway;
    private final ResultTypeGateway resultTypeGateway;
    private final WeaponGateway weaponGateway;
    private final WeaponTypeGateway weaponTypeGateway;
    private final WeaponBrandGateway weaponBrandGateway;
    private final WeaponModelGateway weaponModelGateway;
    private final WeaponCaliberGateway weaponCaliberGateway;
    private final AmmunitionGateway ammunitionGateway;
    private final AmmunitionManufacturerGateway ammunitionManufacturerGateway;
    private final AccessoryGateway accessoryGateway;
    private final AccessoryTypeGateway accessoryTypeGateway;
    private final AccessoryWeaponGateway accessoryWeaponGateway;
    private final TrainingLocationGateway trainingLocationGateway;
    private final VisitGateway visitGateway;
    private final TrainingGateway trainingGateway;
    private final SeriesGateway seriesGateway;
    private final SeriesResultGateway seriesResultGateway;
    private final AthleteHabitualityGateway athleteHabitualityGateway;
    private final HabitualityRuleGateway habitualityRuleGateway;
    private final UserDataExportMapper mapper;

    public UserDataExportResponse export(UUID userId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED));

        List<Weapon> weapons = weaponGateway.findAllByUserId(userId);
        Catalogs catalogs = loadCatalogs(ids(weapons, Weapon::getModelId));

        Map<UUID, WeaponExport> weaponExportById = weapons.stream()
                .collect(Collectors.toMap(Weapon::getId, weapon -> toWeaponExport(weapon, catalogs)));
        List<Ammunition> ammunitions = ammunitionGateway.findAllByUserId(userId);
        Map<UUID, AmmunitionExport> ammunitionExportById = ammunitions.stream()
                .collect(Collectors.toMap(Ammunition::getId, ammunition -> toAmmunitionExport(ammunition, catalogs)));

        return new UserDataExportResponse(
                Instant.now(),
                mapper.toProfile(user),
                practicedModalities(userId, catalogs),
                weapons.stream().map(weapon -> weaponExportById.get(weapon.getId())).toList(),
                ammunitions.stream().map(ammunition -> ammunitionExportById.get(ammunition.getId())).toList(),
                accessories(userId, catalogs, weaponExportById),
                trainingLocationGateway.findAllByUserId(userId).stream().map(mapper::toTrainingLocation).toList(),
                visits(userId, catalogs, weaponExportById, ammunitionExportById),
                habituality(userId));
    }

    private HabitualityExport habituality(UUID userId) {
        TrackingExport tracking = athleteHabitualityGateway.findTrackingByUserId(userId)
                .flatMap(t -> habitualityRuleGateway.findById(t.getRuleId())
                        .map(rule -> new TrackingExport(rule.getName(), t.getLevelName())))
                .orElse(null);
        Integer goal = athleteHabitualityGateway.findGoalByUserId(userId)
                .map(g -> g.getYearlyTrainings())
                .orElse(null);
        return new HabitualityExport(tracking, goal);
    }

    private List<PracticedModalityExport> practicedModalities(UUID userId, Catalogs catalogs) {
        Map<UUID, List<String>> resultTypesByModalityId = modalityResultTypeSelectionGateway.findAllByUserId(userId).stream()
                .collect(Collectors.groupingBy(ModalityResultTypeSelection::getModalityId,
                        Collectors.mapping(selection -> catalogs.resultTypes().get(selection.getResultTypeId()).getName(),
                                Collectors.toList())));

        return practicedModalityGateway.findAllByUserId(userId).stream()
                .map(practiced -> mapper.toPracticedModality(
                        catalogs.modalities().get(practiced.getModalityId()).getName(),
                        resultTypesByModalityId.getOrDefault(practiced.getModalityId(), List.of())))
                .toList();
    }

    private List<AccessoryExport> accessories(UUID userId, Catalogs catalogs, Map<UUID, WeaponExport> weaponExportById) {
        List<Accessory> accessories = accessoryGateway.findAllByUserId(userId);
        Map<UUID, List<AccessoryWeapon>> associationsByAccessoryId = accessoryWeaponGateway
                .findAllByAccessoryIdIn(ids(accessories, Accessory::getId)).stream()
                .collect(Collectors.groupingBy(AccessoryWeapon::getAccessoryId));

        return accessories.stream()
                .map(accessory -> mapper.toAccessory(accessory,
                        catalogs.accessoryTypes().get(accessory.getTypeId()),
                        associationsByAccessoryId.getOrDefault(accessory.getId(), List.of()).stream()
                                .map(association -> weaponExportById.get(association.getWeaponId()))
                                .toList()))
                .toList();
    }

    private List<VisitExport> visits(UUID userId, Catalogs catalogs, Map<UUID, WeaponExport> weaponExportById,
                                     Map<UUID, AmmunitionExport> ammunitionExportById) {
        List<Visit> visits = visitGateway.findAllByUserId(userId);
        Map<UUID, String> locationNameById = trainingLocationGateway.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(TrainingLocation::getId, TrainingLocation::getName));

        List<Training> trainings = trainingGateway.findAllByVisitIdIn(ids(visits, Visit::getId));
        List<Series> series = seriesGateway.findAllByTrainingIdIn(ids(trainings, Training::getId));
        List<SeriesResult> results = seriesResultGateway.findAllBySeriesIdIn(ids(series, Series::getId));

        Map<UUID, List<ResultExport>> resultsBySeriesId = results.stream()
                .collect(Collectors.groupingBy(SeriesResult::getSeriesId,
                        Collectors.mapping(result -> mapper.toResult(result, catalogs.resultTypes().get(result.getResultTypeId())),
                                Collectors.toList())));
        Map<UUID, List<SeriesExport>> seriesByTrainingId = series.stream()
                .sorted(Comparator.comparing(Series::getCreatedAt))
                .collect(Collectors.groupingBy(Series::getTrainingId,
                        Collectors.mapping(s -> mapper.toSeries(s,
                                        s.getWeaponId() == null ? null : weaponExportById.get(s.getWeaponId()),
                                        s.getAmmunitionId() == null ? null : ammunitionExportById.get(s.getAmmunitionId()),
                                        resultsBySeriesId.getOrDefault(s.getId(), List.of())),
                                Collectors.toList())));
        Map<UUID, List<TrainingExport>> trainingsByVisitId = trainings.stream()
                .sorted(Comparator.comparing(Training::getCreatedAt))
                .collect(Collectors.groupingBy(Training::getVisitId,
                        Collectors.mapping(t -> mapper.toTraining(t,
                                        catalogs.modalities().get(t.getModalityId()).getName(),
                                        seriesByTrainingId.getOrDefault(t.getId(), List.of())),
                                Collectors.toList())));

        return visits.stream()
                .sorted(Comparator.comparing(Visit::getCreatedAt))
                .map(visit -> mapper.toVisit(visit, locationNameById.get(visit.getTrainingLocationId()),
                        trainingsByVisitId.getOrDefault(visit.getId(), List.of())))
                .toList();
    }

    private WeaponExport toWeaponExport(Weapon weapon, Catalogs catalogs) {
        return mapper.toWeapon(weapon,
                catalogs.weaponTypes().get(weapon.getTypeId()),
                catalogs.weaponBrands().get(weapon.getBrandId()),
                catalogs.weaponModels().get(weapon.getModelId()),
                catalogs.weaponCalibers().get(weapon.getCaliberId()));
    }

    private AmmunitionExport toAmmunitionExport(Ammunition ammunition, Catalogs catalogs) {
        return mapper.toAmmunition(ammunition,
                ammunition.getManufacturerId() == null ? null : catalogs.manufacturers().get(ammunition.getManufacturerId()),
                ammunition.getCaliberId() == null ? null : catalogs.weaponCalibers().get(ammunition.getCaliberId()));
    }

    private Catalogs loadCatalogs(Collection<UUID> weaponModelIds) {
        return new Catalogs(
                indexById(modalityGateway.findAll(), Modality::getId),
                indexById(resultTypeGateway.findAll(), ResultType::getId),
                indexById(weaponTypeGateway.findAll(), WeaponType::getId),
                indexById(weaponBrandGateway.findAll(), WeaponBrand::getId),
                indexById(weaponModelGateway.findAllById(weaponModelIds), WeaponModel::getId),
                indexById(weaponCaliberGateway.findAll(), WeaponCaliber::getId),
                indexById(ammunitionManufacturerGateway.findAll(), AmmunitionManufacturer::getId),
                indexById(accessoryTypeGateway.findAll(), AccessoryType::getId));
    }

    private static <T> Map<UUID, T> indexById(Collection<T> items, Function<T, UUID> idOf) {
        return items.stream().collect(Collectors.toMap(idOf, Function.identity()));
    }

    private static <T> List<UUID> ids(Collection<T> items, Function<T, UUID> idOf) {
        return items.stream().map(idOf).toList();
    }

    private record Catalogs(
            Map<UUID, Modality> modalities,
            Map<UUID, ResultType> resultTypes,
            Map<UUID, WeaponType> weaponTypes,
            Map<UUID, WeaponBrand> weaponBrands,
            Map<UUID, WeaponModel> weaponModels,
            Map<UUID, WeaponCaliber> weaponCalibers,
            Map<UUID, AmmunitionManufacturer> manufacturers,
            Map<UUID, AccessoryType> accessoryTypes
    ) {
    }
}
