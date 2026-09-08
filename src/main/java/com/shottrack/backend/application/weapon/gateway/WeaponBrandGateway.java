package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.model.WeaponBrand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeaponBrandGateway {

    List<WeaponBrand> findAll();

    Optional<WeaponBrand> findById(UUID id);

    boolean existsById(UUID id);
}
