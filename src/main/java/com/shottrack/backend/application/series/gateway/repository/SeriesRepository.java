package com.shottrack.backend.application.series.gateway.repository;

import com.shottrack.backend.application.series.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeriesRepository extends JpaRepository<Series, UUID> {

    List<Series> findAllByTrainingId(UUID trainingId);
}
