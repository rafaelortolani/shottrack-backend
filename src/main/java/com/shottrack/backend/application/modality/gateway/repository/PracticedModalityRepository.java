package com.shottrack.backend.application.modality.gateway.repository;

import com.shottrack.backend.application.modality.model.PracticedModality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticedModalityRepository extends JpaRepository<PracticedModality, UUID> {

    List<PracticedModality> findAllByUserId(UUID userId);

    Optional<PracticedModality> findByUserIdAndModalityId(UUID userId, UUID modalityId);

    boolean existsByUserIdAndModalityId(UUID userId, UUID modalityId);
}
