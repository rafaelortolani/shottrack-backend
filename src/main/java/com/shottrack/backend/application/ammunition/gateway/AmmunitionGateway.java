package com.shottrack.backend.application.ammunition.gateway;

import com.shottrack.backend.application.ammunition.model.Ammunition;

import java.util.List;
import java.util.UUID;

public interface AmmunitionGateway {

    Ammunition save(Ammunition ammunition);

    List<Ammunition> findAllByUserId(UUID userId);
}
