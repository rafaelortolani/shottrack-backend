package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.model.WeaponType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeaponTypeGateway {

    List<WeaponType> findAll();

    Optional<WeaponType> findById(UUID id);
}
