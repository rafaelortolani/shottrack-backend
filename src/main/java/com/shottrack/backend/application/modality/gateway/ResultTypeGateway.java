package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.model.ResultType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResultTypeGateway {

    List<ResultType> findAll();

    Optional<ResultType> findById(UUID id);
}
