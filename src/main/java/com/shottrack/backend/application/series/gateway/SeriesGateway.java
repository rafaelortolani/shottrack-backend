package com.shottrack.backend.application.series.gateway;

import com.shottrack.backend.application.series.model.Series;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface SeriesGateway {

    Series save(Series series);

    Optional<Series> findById(UUID id);

    List<Series> findAllByTrainingId(UUID trainingId);

    boolean existsByWeaponId(UUID weaponId);

    boolean existsByAmmunitionId(UUID ammunitionId);

    void delete(Series series);

    List<Series> findAllByTrainingIdIn(Collection<UUID> trainingIds);
    void deleteAllByTrainingIdIn(Collection<UUID> trainingIds);
}
