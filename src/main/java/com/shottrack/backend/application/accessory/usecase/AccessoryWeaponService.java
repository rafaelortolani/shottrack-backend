package com.shottrack.backend.application.accessory.usecase;

import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.gateway.AccessoryWeaponGateway;
import com.shottrack.backend.application.accessory.model.Accessory;
import com.shottrack.backend.application.accessory.model.AccessoryWeapon;
import com.shottrack.backend.application.weapon.usecase.WeaponService;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * UC19/ADR-0008: associação N:N entre acessório e arma. Endpoints separados
 * do CRUD do acessório em si — mesmo raciocínio já aplicado a modalidades
 * praticadas (ADR-0005/UC12).
 */
@Service
@RequiredArgsConstructor
public class AccessoryWeaponService {

    private final AccessoryWeaponGateway accessoryWeaponGateway;
    private final AccessoryService accessoryService;
    private final WeaponService weaponService;

    public AccessoryResponse associate(UUID userId, UUID accessoryId, UUID weaponId) {
        Accessory accessory = accessoryService.findOwnedAccessoryOrThrow(userId, accessoryId);
        weaponService.findOwnedWeaponOrThrow(userId, weaponId);

        if (accessoryWeaponGateway.existsByAccessoryIdAndWeaponId(accessoryId, weaponId)) {
            throw new BusinessException("ACCESSORY_ALREADY_ASSOCIATED", HttpStatus.CONFLICT);
        }

        accessoryWeaponGateway.save(AccessoryWeapon.builder()
                .accessoryId(accessoryId)
                .weaponId(weaponId)
                .build());

        return accessoryService.toResponseWithWeapons(accessory);
    }

    public AccessoryResponse disassociate(UUID userId, UUID accessoryId, UUID weaponId) {
        Accessory accessory = accessoryService.findOwnedAccessoryOrThrow(userId, accessoryId);
        weaponService.findOwnedWeaponOrThrow(userId, weaponId);

        AccessoryWeapon association = accessoryWeaponGateway.findByAccessoryIdAndWeaponId(accessoryId, weaponId)
                .orElseThrow(() -> new BusinessException("ACCESSORY_NOT_ASSOCIATED", HttpStatus.NOT_FOUND));

        accessoryWeaponGateway.delete(association);

        return accessoryService.toResponseWithWeapons(accessory);
    }
}
