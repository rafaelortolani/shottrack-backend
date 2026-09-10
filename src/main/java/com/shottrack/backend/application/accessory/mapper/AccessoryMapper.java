package com.shottrack.backend.application.accessory.mapper;

import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.model.Accessory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccessoryMapper {

    AccessoryResponse toResponse(Accessory accessory);
}
