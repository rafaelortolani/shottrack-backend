package com.shottrack.backend.application.visit.gateway.repository;

import com.shottrack.backend.application.visit.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {

    List<Visit> findAllByUserId(UUID userId);

    boolean existsByTrainingLocationId(UUID trainingLocationId);
}
