package com.shottrack.backend.application.ammunition.gateway.repository;

import com.shottrack.backend.application.ammunition.model.Ammunition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmmunitionRepository extends JpaRepository<Ammunition, UUID> {

    List<Ammunition> findAllByUserId(UUID userId);
}
