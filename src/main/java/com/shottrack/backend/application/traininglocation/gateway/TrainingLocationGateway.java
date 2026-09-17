package com.shottrack.backend.application.traininglocation.gateway;

import com.shottrack.backend.application.traininglocation.model.TrainingLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingLocationGateway {

    TrainingLocation save(TrainingLocation trainingLocation);

    List<TrainingLocation> findAllByUserId(UUID userId);

    Optional<TrainingLocation> findById(UUID id);

    void delete(TrainingLocation trainingLocation);
}
