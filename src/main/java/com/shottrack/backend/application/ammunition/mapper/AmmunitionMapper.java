package com.shottrack.backend.application.ammunition.mapper;

import com.shottrack.backend.application.ammunition.dto.AmmunitionManufacturerResponse;
import com.shottrack.backend.application.ammunition.dto.AmmunitionResponse;
import com.shottrack.backend.application.ammunition.model.Ammunition;
import com.shottrack.backend.application.ammunition.model.AmmunitionManufacturer;
import com.shottrack.backend.application.weapon.dto.WeaponCaliberResponse;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AmmunitionMapper {

    @Mapping(target = "id", source = "ammunition.id")
    @Mapping(target = "nickname", source = "ammunition.nickname")
    @Mapping(target = "projectileWeightGrains", source = "ammunition.projectileWeightGrains")
    @Mapping(target = "powderCharge", source = "ammunition.powderCharge")
    @Mapping(target = "projectileType", source = "ammunition.projectileType")
    @Mapping(target = "lot", source = "ammunition.lot")
    @Mapping(target = "notes", source = "ammunition.notes")
    @Mapping(target = "createdAt", source = "ammunition.createdAt")
    @Mapping(target = "manufacturer", source = "manufacturer")
    @Mapping(target = "caliber", source = "caliber")
    AmmunitionResponse toResponse(Ammunition ammunition, AmmunitionManufacturer manufacturer, WeaponCaliber caliber);

    AmmunitionManufacturerResponse toResponse(AmmunitionManufacturer manufacturer);

    WeaponCaliberResponse toResponse(WeaponCaliber caliber);
}
