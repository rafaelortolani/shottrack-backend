package com.shottrack.backend.application.series.gateway;

import com.shottrack.backend.application.series.model.SeriesResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeriesResultGateway {

    SeriesResult save(SeriesResult seriesResult);

    Optional<SeriesResult> findBySeriesIdAndResultTypeId(UUID seriesId, UUID resultTypeId);

    List<SeriesResult> findAllBySeriesId(UUID seriesId);

    void delete(SeriesResult seriesResult);
}
