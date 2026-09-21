package com.shottrack.backend.application.series.gateway;

import com.shottrack.backend.application.series.gateway.repository.SeriesRepository;
import com.shottrack.backend.application.series.model.Series;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SeriesGatewayImpl implements SeriesGateway {

    private final SeriesRepository seriesRepository;

    @Override
    public Series save(Series series) {
        return seriesRepository.save(series);
    }

    @Override
    public Optional<Series> findById(UUID id) {
        return seriesRepository.findById(id);
    }

    @Override
    public List<Series> findAllByTrainingId(UUID trainingId) {
        return seriesRepository.findAllByTrainingId(trainingId);
    }

    @Override
    public void delete(Series series) {
        seriesRepository.delete(series);
    }
}
