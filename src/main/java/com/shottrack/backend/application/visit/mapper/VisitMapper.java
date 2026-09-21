package com.shottrack.backend.application.visit.mapper;

import com.shottrack.backend.application.visit.dto.TrainingSummaryResponse;
import com.shottrack.backend.application.visit.dto.VisitResponse;
import com.shottrack.backend.application.visit.model.Visit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VisitMapper {

    @Mapping(target = "startedAt", source = "visit.createdAt")
    VisitResponse toResponse(Visit visit, List<TrainingSummaryResponse> trainings);
}
