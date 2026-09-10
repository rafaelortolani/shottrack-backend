package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.gateway.repository.AccessoryWeaponRepository;
import com.shottrack.backend.application.accessory.model.AccessoryWeapon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AccessoryWeaponGatewayImpl implements AccessoryWeaponGateway {

    private final AccessoryWeaponRepository accessoryWeaponRepository;

    @Override
    public AccessoryWeapon save(AccessoryWeapon accessoryWeapon) {
        return accessoryWeaponRepository.save(accessoryWeapon);
    }

    @Override
    public List<AccessoryWeapon> findAllByAccessoryId(UUID accessoryId) {
        return accessoryWeaponRepository.findAllByAccessoryId(accessoryId);
    }

    @Override
    public Optional<AccessoryWeapon> findByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId) {
        return accessoryWeaponRepository.findByAccessoryIdAndWeaponId(accessoryId, weaponId);
    }

    @Override
    public boolean existsByAccessoryIdAndWeaponId(UUID accessoryId, UUID weaponId) {
        return accessoryWeaponRepository.existsByAccessoryIdAndWeaponId(accessoryId, weaponId);
    }

    @Override
    public void delete(AccessoryWeapon accessoryWeapon) {
        accessoryWeaponRepository.delete(accessoryWeapon);
    }

    @Override
    public void deleteAllByAccessoryId(UUID accessoryId) {
        accessoryWeaponRepository.deleteAllByAccessoryId(accessoryId);
    }
}
