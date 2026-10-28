package com.shottrack.backend.application.visit.gateway;

import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.TrainingStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingGateway {

    Training save(Training training);

    Optional<Training> findById(UUID id);

    List<Training> findAllByVisitId(UUID visitId);

    List<Training> findAllByVisitIdAndStatus(UUID visitId, TrainingStatus status);

    boolean existsByVisitIdInAndModalityId(Collection<UUID> visitIds, UUID modalityId);

    /** ADR-0022: período [from, to) pelo início do treino; modalityId nulo = todas. */
    long countByUserId(UUID userId, UUID modalityId, Instant from, Instant to);

    long countDistinctModalitiesByUserId(UUID userId, Instant from, Instant to);

    void delete(Training training);

    List<Training> findAllByVisitIdIn(Collection<UUID> visitIds);
    void deleteAllByVisitIdIn(Collection<UUID> visitIds);
}
