package com.shottrack.backend.application.series.gateway;

import com.shottrack.backend.application.series.gateway.repository.SeriesResultRepository;
import com.shottrack.backend.application.series.model.SeriesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SeriesResultGatewayImpl implements SeriesResultGateway {

    private final SeriesResultRepository seriesResultRepository;

    @Override
    public SeriesResult save(SeriesResult seriesResult) {
        return seriesResultRepository.save(seriesResult);
    }

    @Override
    public Optional<SeriesResult> findBySeriesIdAndResultTypeId(UUID seriesId, UUID resultTypeId) {
        return seriesResultRepository.findBySeriesIdAndResultTypeId(seriesId, resultTypeId);
    }

    @Override
    public List<SeriesResult> findAllBySeriesId(UUID seriesId) {
        return seriesResultRepository.findAllBySeriesId(seriesId);
    }

    @Override
    public void delete(SeriesResult seriesResult) {
        seriesResultRepository.delete(seriesResult);
    }
}
