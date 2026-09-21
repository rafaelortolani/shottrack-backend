package com.shottrack.backend.application.visit.gateway.repository;

import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.TrainingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainingRepository extends JpaRepository<Training, UUID> {

    List<Training> findAllByVisitId(UUID visitId);

    List<Training> findAllByVisitIdAndStatus(UUID visitId, TrainingStatus status);
}
