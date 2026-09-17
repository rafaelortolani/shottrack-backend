package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.model.AccessoryType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessoryTypeGateway {

    Optional<AccessoryType> findById(UUID id);

    List<AccessoryType> findAll();
}
