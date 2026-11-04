package com.shottrack.backend.application.seriesphoto.gateway;

import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SeriesPhotoGateway {

    SeriesPhoto save(SeriesPhoto photo);

    Optional<SeriesPhoto> findById(UUID id);

    List<SeriesPhoto> findAllBySeriesId(UUID seriesId);

    List<SeriesPhoto> findAllBySeriesIdIn(Collection<UUID> seriesIds);

    List<SeriesPhoto> findAllByAthleteId(UUID athleteId);

    List<SeriesPhoto> findAllByTrainingId(UUID trainingId);

    List<SeriesPhoto> findAllByVisitId(UUID visitId);

    long countBySeriesId(UUID seriesId);

    long countByAthleteId(UUID athleteId);

    /** Próxima posição livre da série (maior atual + 1). */
    int nextPosition(UUID seriesId);

    /** Série → quantidade de fotos; séries sem foto não aparecem no mapa. */
    Map<UUID, Long> countBySeriesIds(Collection<UUID> seriesIds);

    boolean existsByAnyKey(String key);

    void delete(SeriesPhoto photo);

    void deleteAllByAthleteId(UUID athleteId);
}
