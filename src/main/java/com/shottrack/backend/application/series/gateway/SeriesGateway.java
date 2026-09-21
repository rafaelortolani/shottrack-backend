package com.shottrack.backend.application.series.gateway;

import com.shottrack.backend.application.series.model.Series;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeriesGateway {

    Series save(Series series);

    Optional<Series> findById(UUID id);

    List<Series> findAllByTrainingId(UUID trainingId);

    void delete(Series series);
}
