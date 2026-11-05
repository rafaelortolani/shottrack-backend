package com.shottrack.backend.application.seriesphoto.gateway;

import com.shottrack.backend.application.seriesphoto.gateway.repository.SeriesPhotoRepository;
import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class SeriesPhotoGatewayImpl implements SeriesPhotoGateway {

    private final SeriesPhotoRepository repository;

    @Override
    public SeriesPhoto save(SeriesPhoto photo) {
        return repository.save(photo);
    }

    @Override
    public Optional<SeriesPhoto> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<SeriesPhoto> findAllBySeriesId(UUID seriesId) {
        return repository.findAllBySeriesIdOrderByPositionAscCreatedAtAsc(seriesId);
    }

    @Override
    public List<SeriesPhoto> findAllBySeriesIdIn(Collection<UUID> seriesIds) {
        return seriesIds.isEmpty() ? List.of() : repository.findAllBySeriesIdIn(seriesIds);
    }

    @Override
    public List<SeriesPhoto> findAllByAthleteId(UUID athleteId) {
        return repository.findAllByAthleteId(athleteId);
    }

    @Override
    public List<SeriesPhoto> findAllByTrainingId(UUID trainingId) {
        return repository.findAllByTrainingId(trainingId);
    }

    @Override
    public List<SeriesPhoto> findAllByVisitId(UUID visitId) {
        return repository.findAllByVisitId(visitId);
    }

    @Override
    public long countBySeriesId(UUID seriesId) {
        return repository.countBySeriesId(seriesId);
    }

    @Override
    public long countByAthleteId(UUID athleteId) {
        return repository.countByAthleteId(athleteId);
    }

    @Override
    public int nextPosition(UUID seriesId) {
        return repository.maxPositionBySeriesId(seriesId) + 1;
    }

    @Override
    public Map<UUID, Long> countBySeriesIds(Collection<UUID> seriesIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (seriesIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : repository.countGroupedBySeriesIdIn(seriesIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public Usage usageOf(UUID athleteId) {
        Object[] row = repository.usageByAthleteId(athleteId).get(0);
        return new Usage(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }

    @Override
    public Map<UUID, Usage> usageOf(Collection<UUID> athleteIds) {
        if (athleteIds.isEmpty()) {
            return Map.of();
        }
        return repository.usageByAthleteIds(athleteIds).stream().collect(Collectors.toMap(
                row -> (UUID) row[0], row -> new Usage(((Number) row[1]).longValue(), ((Number) row[2]).longValue())));
    }

    @Override
    public void lockAthleteUploads(UUID athleteId) {
        repository.advisoryLock(athleteId.getMostSignificantBits() ^ athleteId.getLeastSignificantBits());
    }

    @Override
    public boolean existsByAnyKey(String key) {
        return repository.existsByAnyKey(key);
    }

    @Override
    public void delete(SeriesPhoto photo) {
        repository.delete(photo);
    }

    @Override
    @Transactional
    public void deleteAllByAthleteId(UUID athleteId) {
        repository.deleteAllByAthleteId(athleteId);
    }
}
