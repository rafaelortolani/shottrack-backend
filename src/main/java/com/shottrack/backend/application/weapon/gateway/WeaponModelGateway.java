package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.model.WeaponModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeaponModelGateway {

    List<WeaponModel> findAllByBrandId(UUID brandId);

    Optional<WeaponModel> findById(UUID id);
}
