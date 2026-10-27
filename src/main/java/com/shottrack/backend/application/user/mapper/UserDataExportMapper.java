package com.shottrack.backend.application.user.mapper;

import com.shottrack.backend.application.accessory.model.Accessory;
import com.shottrack.backend.application.accessory.model.AccessoryType;
import com.shottrack.backend.application.ammunition.model.Ammunition;
import com.shottrack.backend.application.ammunition.model.AmmunitionManufacturer;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.series.model.SeriesResult;
import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.AccessoryExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.AmmunitionExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.PracticedModalityExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.ProfileExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.ResultExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.SeriesExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.TrainingExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.TrainingLocationExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.VisitExport;
import com.shottrack.backend.application.user.dto.UserDataExportResponse.WeaponExport;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.weapon.model.Weapon;
import com.shottrack.backend.application.weapon.model.WeaponBrand;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import com.shottrack.backend.application.weapon.model.WeaponType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * UC53: model → DTO de exportação. Cada método recebe, além da entidade, os
 * registros de catálogo já resolvidos pelo usecase (pra não consultar o banco
 * por item) — por isso os mapeamentos são todos explícitos: vários dos tipos
 * de origem têm propriedades com o mesmo nome (ex: name).
 */
@Mapper(componentModel = "spring")
public interface UserDataExportMapper {

    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "experienceLevel", source = "experienceLevel")
    @Mapping(target = "registeredAt", source = "createdAt")
    @Mapping(target = "termsAcceptedAt", source = "termsAcceptedAt")
    @Mapping(target = "termsVersion", source = "termsVersion")
    ProfileExport toProfile(User user);

    @Mapping(target = "modality", source = "modality")
    @Mapping(target = "resultTypes", source = "resultTypes")
    PracticedModalityExport toPracticedModality(String modality, List<String> resultTypes);

    @Mapping(target = "type", source = "type.name")
    @Mapping(target = "brand", source = "brand.name")
    @Mapping(target = "model", source = "model.name")
    @Mapping(target = "caliber", source = "caliber.name")
    @Mapping(target = "nickname", source = "weapon.nickname")
    WeaponExport toWeapon(Weapon weapon, WeaponType type, WeaponBrand brand, WeaponModel model, WeaponCaliber caliber);

    @Mapping(target = "manufacturer", source = "manufacturer.name")
    @Mapping(target = "caliber", source = "caliber.name")
    @Mapping(target = "nickname", source = "ammunition.nickname")
    @Mapping(target = "projectileWeightGrains", source = "ammunition.projectileWeightGrains")
    @Mapping(target = "powderCharge", source = "ammunition.powderCharge")
    @Mapping(target = "projectileType", source = "ammunition.projectileType")
    @Mapping(target = "lot", source = "ammunition.lot")
    @Mapping(target = "notes", source = "ammunition.notes")
    AmmunitionExport toAmmunition(Ammunition ammunition, AmmunitionManufacturer manufacturer, WeaponCaliber caliber);

    @Mapping(target = "name", source = "accessory.name")
    @Mapping(target = "type", source = "type.name")
    @Mapping(target = "notes", source = "accessory.notes")
    @Mapping(target = "weapons", source = "weapons")
    AccessoryExport toAccessory(Accessory accessory, AccessoryType type, List<WeaponExport> weapons);

    TrainingLocationExport toTrainingLocation(TrainingLocation trainingLocation);

    @Mapping(target = "trainingLocation", source = "trainingLocation")
    @Mapping(target = "startedAt", source = "visit.createdAt")
    @Mapping(target = "endedAt", source = "visit.endedAt")
    @Mapping(target = "status", source = "visit.status")
    @Mapping(target = "observations", source = "visit.observations")
    @Mapping(target = "trainings", source = "trainings")
    VisitExport toVisit(Visit visit, String trainingLocation, List<TrainingExport> trainings);

    @Mapping(target = "modality", source = "modality")
    @Mapping(target = "startedAt", source = "training.createdAt")
    @Mapping(target = "endedAt", source = "training.endedAt")
    @Mapping(target = "status", source = "training.status")
    @Mapping(target = "series", source = "series")
    TrainingExport toTraining(Training training, String modality, List<SeriesExport> series);

    @Mapping(target = "weapon", source = "weapon")
    @Mapping(target = "ammunition", source = "ammunition")
    @Mapping(target = "distanceMeters", source = "series.distanceMeters")
    @Mapping(target = "target", source = "series.target")
    @Mapping(target = "shotCount", source = "series.shotCount")
    @Mapping(target = "notes", source = "series.notes")
    @Mapping(target = "results", source = "results")
    SeriesExport toSeries(Series series, WeaponExport weapon, AmmunitionExport ammunition, List<ResultExport> results);

    @Mapping(target = "resultType", source = "type.name")
    @Mapping(target = "value", source = "result.value")
    @Mapping(target = "notApplicable", source = "result.notApplicable")
    ResultExport toResult(SeriesResult result, ResultType type);
}
