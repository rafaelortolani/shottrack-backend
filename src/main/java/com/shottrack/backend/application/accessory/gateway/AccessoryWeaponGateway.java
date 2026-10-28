package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.model.AccessoryWeapon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface AccessoryWeaponGateway {

    AccessoryWeapon save(AccessoryWeapon accessoryWeapon);

    List<AccessoryWeapon> findAllByAccessoryId(UUID accessoryId);

    Optional<AccessoryWeapon> findByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId);

    boolean existsByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId);

    void delete(AccessoryWeapon accessoryWeapon);

    List<AccessoryWeapon> findAllByAccessoryIdIn(Collection<UUID> accessoryIds);
    void deleteAllByAccessoryIdIn(Collection<UUID> accessoryIds);
    void deleteAllByWeaponIdIn(Collection<UUID> weaponIds);
}
