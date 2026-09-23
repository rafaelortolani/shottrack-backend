package com.shottrack.backend.application.visit.gateway;

import com.shottrack.backend.application.visit.model.Visit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitGateway {

    Visit save(Visit visit);

    List<Visit> findAllByUserId(UUID userId);

    Optional<Visit> findById(UUID id);

    boolean existsByTrainingLocationId(UUID trainingLocationId);

    void delete(Visit visit);
}
