package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.model.AccessoryWeapon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessoryWeaponGateway {

    AccessoryWeapon save(AccessoryWeapon accessoryWeapon);

    List<AccessoryWeapon> findAllByAccessoryId(UUID accessoryId);

    Optional<AccessoryWeapon> findByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId);

    boolean existsByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId);

    void delete(AccessoryWeapon accessoryWeapon);

    void deleteAllByAccessoryId(UUID accessoryId);
}
