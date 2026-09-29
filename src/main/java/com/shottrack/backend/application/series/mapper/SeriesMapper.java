package com.shottrack.backend.application.series.mapper;

import com.shottrack.backend.application.ammunition.dto.AmmunitionResponse;
import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.series.dto.SeriesHistoryResponse;
import com.shottrack.backend.application.series.dto.SeriesResponse;
import com.shottrack.backend.application.series.dto.SeriesResultResponse;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.traininglocation.dto.TrainingLocationResponse;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SeriesMapper {

    SeriesResponse toResponse(Series series, List<SeriesResultResponse> results);

    @Mapping(target = "id", source = "series.id")
    @Mapping(target = "notes", source = "series.notes")
    @Mapping(target = "createdAt", source = "series.createdAt")
    SeriesHistoryResponse toHistoryResponse(Series series, LocalDate date, TrainingLocationResponse trainingLocation,
                                            ModalityResponse modality, WeaponResponse weapon,
                                            AmmunitionResponse ammunition, List<SeriesResultResponse> results);
}
