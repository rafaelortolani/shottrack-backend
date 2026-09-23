package com.shottrack.backend.application.visit.gateway;

import com.shottrack.backend.application.visit.gateway.repository.TrainingRepository;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.TrainingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class TrainingGatewayImpl implements TrainingGateway {

    private final TrainingRepository trainingRepository;

    @Override
    public Training save(Training training) {
        return trainingRepository.save(training);
    }

    @Override
    public Optional<Training> findById(UUID id) {
        return trainingRepository.findById(id);
    }

    @Override
    public List<Training> findAllByVisitId(UUID visitId) {
        return trainingRepository.findAllByVisitId(visitId);
    }

    @Override
    public List<Training> findAllByVisitIdAndStatus(UUID visitId, TrainingStatus status) {
        return trainingRepository.findAllByVisitIdAndStatus(visitId, status);
    }

    @Override
    public void delete(Training training) {
        trainingRepository.delete(training);
    }
}
