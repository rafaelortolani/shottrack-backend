package com.shottrack.backend.application.modality.mapper;

import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.modality.model.Modality;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModalityMapper {

    ModalityResponse toResponse(Modality modality);
}
