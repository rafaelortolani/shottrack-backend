package com.shottrack.backend.application.ammunition.gateway.repository;

import com.shottrack.backend.application.ammunition.model.AmmunitionManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AmmunitionManufacturerRepository extends JpaRepository<AmmunitionManufacturer, UUID> {
}
