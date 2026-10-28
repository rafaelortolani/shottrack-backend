package com.shottrack.backend.application.visit.gateway;

import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.visit.model.VisitType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitGateway {

    Visit save(Visit visit);

    List<Visit> findAllByUserId(UUID userId);

    Optional<Visit> findById(UUID id);

    boolean existsByTrainingLocationId(UUID trainingLocationId);

    void delete(Visit visit);

    void deleteAllByUserId(UUID userId);

    /** ADR-0023: visitas ENCERRADAS com início em [from, to). */
    List<ClosedVisitFact> findClosedFactsByUserIdBetween(UUID userId, Instant from, Instant to);

    long countClosedByUserIdAndTypeBetween(UUID userId, VisitType type, Instant from, Instant to);
}
