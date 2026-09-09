package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.model.Modality;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModalityGateway {

    List<Modality> findAll();

    Optional<Modality> findById(UUID id);
}
