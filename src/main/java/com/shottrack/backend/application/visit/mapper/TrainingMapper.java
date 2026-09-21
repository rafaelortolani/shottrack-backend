package com.shottrack.backend.application.visit.mapper;

import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.visit.dto.TrainingResponse;
import com.shottrack.backend.application.visit.model.Training;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrainingMapper {

    @Mapping(target = "id", source = "training.id")
    @Mapping(target = "modalityId", source = "training.modalityId")
    @Mapping(target = "startedAt", source = "training.createdAt")
    @Mapping(target = "modalityName", source = "modality.name")
    TrainingResponse toResponse(Training training, Modality modality);
}
