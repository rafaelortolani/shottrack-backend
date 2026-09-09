package com.shottrack.backend.application.weapon.mapper;

import com.shottrack.backend.application.weapon.dto.WeaponBrandResponse;
import com.shottrack.backend.application.weapon.dto.WeaponCaliberResponse;
import com.shottrack.backend.application.weapon.dto.WeaponModelResponse;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import com.shottrack.backend.application.weapon.dto.WeaponTypeResponse;
import com.shottrack.backend.application.weapon.model.Weapon;
import com.shottrack.backend.application.weapon.model.WeaponBrand;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import com.shottrack.backend.application.weapon.model.WeaponType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeaponMapper {

    @Mapping(target = "id", source = "weapon.id")
    @Mapping(target = "nickname", source = "weapon.nickname")
    @Mapping(target = "createdAt", source = "weapon.createdAt")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "brand", source = "brand")
    @Mapping(target = "model", source = "model")
    @Mapping(target = "caliber", source = "caliber")
    WeaponResponse toResponse(Weapon weapon, WeaponType type, WeaponBrand brand, WeaponModel model, WeaponCaliber caliber);

    WeaponTypeResponse toResponse(WeaponType type);

    WeaponBrandResponse toResponse(WeaponBrand brand);

    WeaponModelResponse toResponse(WeaponModel model);

    WeaponCaliberResponse toResponse(WeaponCaliber caliber);
}
