package com.shottrack.backend.application.traininglocation.mapper;

import com.shottrack.backend.application.traininglocation.dto.TrainingLocationResponse;
import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrainingLocationMapper {

    TrainingLocationResponse toResponse(TrainingLocation trainingLocation);
}
