package com.shottrack.backend.application.accessory.mapper;

import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.dto.AccessoryTypeResponse;
import com.shottrack.backend.application.accessory.model.Accessory;
import com.shottrack.backend.application.accessory.model.AccessoryType;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccessoryMapper {

    @Mapping(target = "id", source = "accessory.id")
    @Mapping(target = "name", source = "accessory.name")
    @Mapping(target = "notes", source = "accessory.notes")
    @Mapping(target = "createdAt", source = "accessory.createdAt")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "weapons", source = "weapons")
    AccessoryResponse toResponse(Accessory accessory, AccessoryType type, List<WeaponResponse> weapons);

    AccessoryTypeResponse toResponse(AccessoryType type);
}
