package com.shottrack.backend.application.accessory.mapper;

import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.model.Accessory;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccessoryMapper {

    @Mapping(target = "weapons", source = "weapons")
    AccessoryResponse toResponse(Accessory accessory, List<WeaponResponse> weapons);
}
