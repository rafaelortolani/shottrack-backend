package com.shottrack.backend.application.ammunition.gateway;

import com.shottrack.backend.application.ammunition.model.AmmunitionManufacturer;

import java.util.Optional;
import java.util.UUID;

public interface AmmunitionManufacturerGateway {

    Optional<AmmunitionManufacturer> findById(UUID id);
}
