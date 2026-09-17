package com.shottrack.backend.application.traininglocation.gateway;

import com.shottrack.backend.application.traininglocation.gateway.repository.TrainingLocationRepository;
import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class TrainingLocationGatewayImpl implements TrainingLocationGateway {

    private final TrainingLocationRepository trainingLocationRepository;

    @Override
    public TrainingLocation save(TrainingLocation trainingLocation) {
        return trainingLocationRepository.save(trainingLocation);
    }

    @Override
    public List<TrainingLocation> findAllByUserId(UUID userId) {
        return trainingLocationRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<TrainingLocation> findById(UUID id) {
        return trainingLocationRepository.findById(id);
    }

    @Override
    public void delete(TrainingLocation trainingLocation) {
        trainingLocationRepository.delete(trainingLocation);
    }
}
