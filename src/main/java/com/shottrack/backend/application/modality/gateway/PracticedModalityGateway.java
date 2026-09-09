package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.model.PracticedModality;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticedModalityGateway {

    PracticedModality save(PracticedModality practicedModality);

    List<PracticedModality> findAllByUserId(UUID userId);

    Optional<PracticedModality> findByUserIdAndModalityId(UUID userId, UUID modalityId);

    boolean existsByUserIdAndModalityId(UUID userId, UUID modalityId);

    void delete(PracticedModality practicedModality);
}
