package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.model.Weapon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeaponGateway {

    Weapon save(Weapon weapon);

    List<Weapon> findAllByUserId(UUID userId);

    Optional<Weapon> findById(UUID id);

    void delete(Weapon weapon);
}
