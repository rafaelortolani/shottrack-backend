package com.shottrack.backend.application.visit.gateway;

import com.shottrack.backend.application.visit.gateway.repository.VisitRepository;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.visit.model.VisitType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class VisitGatewayImpl implements VisitGateway {

    private final VisitRepository visitRepository;

    @Override
    public Visit save(Visit visit) {
        return visitRepository.save(visit);
    }

    @Override
    public List<Visit> findAllByUserId(UUID userId) {
        return visitRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Visit> findById(UUID id) {
        return visitRepository.findById(id);
    }

    @Override
    public boolean existsByTrainingLocationId(UUID trainingLocationId) {
        return visitRepository.existsByTrainingLocationId(trainingLocationId);
    }

    @Override
    public void delete(Visit visit) {
        visitRepository.delete(visit);
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        visitRepository.deleteAllByUserId(userId);
    }

    @Override
    public List<ClosedVisitFact> findClosedFactsByUserIdBetween(UUID userId, Instant from, Instant to) {
        return visitRepository.findClosedFactsByUserIdBetween(userId, from, to);
    }

    @Override
    public long countClosedByUserIdAndTypeBetween(UUID userId, VisitType type, Instant from, Instant to) {
        return visitRepository.countClosedByUserIdAndTypeBetween(userId, type, from, to);
    }
}
