package com.shottrack.backend.application.series.mapper;

import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.series.dto.SeriesResultResponse;
import com.shottrack.backend.application.series.model.SeriesResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeriesResultMapper {

    @Mapping(target = "resultTypeId", source = "seriesResult.resultTypeId")
    @Mapping(target = "resultTypeName", source = "resultType.name")
    SeriesResultResponse toResponse(SeriesResult seriesResult, ResultType resultType);
}
