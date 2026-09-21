package com.shottrack.backend.application.visit.gateway;

import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.TrainingStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingGateway {

    Training save(Training training);

    Optional<Training> findById(UUID id);

    List<Training> findAllByVisitId(UUID visitId);

    List<Training> findAllByVisitIdAndStatus(UUID visitId, TrainingStatus status);
}
