package com.shottrack.backend.application.modality.gateway.repository;

import com.shottrack.backend.application.modality.model.Modality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModalityRepository extends JpaRepository<Modality, UUID> {
}
