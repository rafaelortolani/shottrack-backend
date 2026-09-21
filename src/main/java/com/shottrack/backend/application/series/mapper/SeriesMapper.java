package com.shottrack.backend.application.series.mapper;

import com.shottrack.backend.application.series.dto.SeriesResponse;
import com.shottrack.backend.application.series.dto.SeriesResultResponse;
import com.shottrack.backend.application.series.model.Series;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeriesMapper {

    SeriesResponse toResponse(Series series, List<SeriesResultResponse> results);
}
