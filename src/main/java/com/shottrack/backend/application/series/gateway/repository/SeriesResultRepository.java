package com.shottrack.backend.application.series.gateway.repository;

import com.shottrack.backend.application.series.model.SeriesResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeriesResultRepository extends JpaRepository<SeriesResult, UUID> {

    Optional<SeriesResult> findBySeriesIdAndResultTypeId(UUID seriesId, UUID resultTypeId);

    List<SeriesResult> findAllBySeriesId(UUID seriesId);
}
