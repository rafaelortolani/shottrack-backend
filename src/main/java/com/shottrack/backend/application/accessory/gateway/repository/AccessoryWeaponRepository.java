package com.shottrack.backend.application.accessory.gateway.repository;

import com.shottrack.backend.application.accessory.model.AccessoryWeapon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessoryWeaponRepository extends JpaRepository<AccessoryWeapon, UUID> {

    List<AccessoryWeapon> findAllByAccessoryId(UUID accessoryId);

    Optional<AccessoryWeapon> findByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId);

    boolean existsByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId);

    void deleteAllByAccessoryId(UUID accessoryId);
}
