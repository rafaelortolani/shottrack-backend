package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.model.WeaponCaliber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeaponCaliberGateway {

    List<WeaponCaliber> findAll();

    Optional<WeaponCaliber> findById(UUID id);
}
