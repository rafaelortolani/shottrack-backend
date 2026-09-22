package com.shottrack.backend.application.modality.gateway.repository;

import com.shottrack.backend.application.modality.model.ResultType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResultTypeRepository extends JpaRepository<ResultType, UUID> {

    Optional<ResultType> findByName(String name);
}
