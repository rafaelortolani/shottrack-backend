package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.gateway.repository.PracticedModalityRepository;
import com.shottrack.backend.application.modality.model.PracticedModality;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PracticedModalityGatewayImpl implements PracticedModalityGateway {

    private final PracticedModalityRepository practicedModalityRepository;

    @Override
    public PracticedModality save(PracticedModality practicedModality) {
        return practicedModalityRepository.save(practicedModality);
    }

    @Override
    public List<PracticedModality> findAllByUserId(UUID userId) {
        return practicedModalityRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<PracticedModality> findByUserIdAndModalityId(UUID userId, UUID modalityId) {
        return practicedModalityRepository.findByUserIdAndModalityId(userId, modalityId);
    }

    @Override
    public boolean existsByUserIdAndModalityId(UUID userId, UUID modalityId) {
        return practicedModalityRepository.existsByUserIdAndModalityId(userId, modalityId);
    }

    @Override
    public void delete(PracticedModality practicedModality) {
        practicedModalityRepository.delete(practicedModality);
    }
}
