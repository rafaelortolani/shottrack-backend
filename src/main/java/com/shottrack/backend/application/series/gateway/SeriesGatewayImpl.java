package com.shottrack.backend.application.series.gateway;

import com.shottrack.backend.application.series.gateway.repository.SeriesRepository;
import com.shottrack.backend.application.series.model.Series;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

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
    public boolean existsByWeaponId(UUID weaponId) {
        return seriesRepository.existsByWeaponId(weaponId);
    }

    @Override
    public boolean existsByAmmunitionId(UUID ammunitionId) {
        return seriesRepository.existsByAmmunitionId(ammunitionId);
    }

    @Override
    public void delete(Series series) {
        seriesRepository.delete(series);
    }

    @Override
    public List<Series> findAllByTrainingIdIn(Collection<UUID> trainingIds) {
        return seriesRepository.findAllByTrainingIdIn(trainingIds);
    }

    @Override
    public void deleteAllByTrainingIdIn(Collection<UUID> trainingIds) {
        seriesRepository.deleteAllByTrainingIdIn(trainingIds);
    }
}
