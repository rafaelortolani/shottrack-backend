package com.shottrack.backend.application.traininglocation.gateway.repository;

import com.shottrack.backend.application.traininglocation.model.TrainingLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainingLocationRepository extends JpaRepository<TrainingLocation, UUID> {

    List<TrainingLocation> findAllByUserId(UUID userId);
}
