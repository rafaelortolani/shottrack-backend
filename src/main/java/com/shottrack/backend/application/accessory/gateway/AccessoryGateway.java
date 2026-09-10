package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.model.Accessory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessoryGateway {

    Accessory save(Accessory accessory);

    List<Accessory> findAllByUserId(UUID userId);

    Optional<Accessory> findById(UUID id);

    void delete(Accessory accessory);
}
