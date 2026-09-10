package com.shottrack.backend.application.ammunition.usecase;

import com.shottrack.backend.application.ammunition.dto.AmmunitionRegisterRequest;
import com.shottrack.backend.application.ammunition.dto.AmmunitionResponse;
import com.shottrack.backend.application.ammunition.gateway.AmmunitionGateway;
import com.shottrack.backend.application.ammunition.gateway.AmmunitionManufacturerGateway;
import com.shottrack.backend.application.ammunition.mapper.AmmunitionMapper;
import com.shottrack.backend.application.ammunition.model.Ammunition;
import com.shottrack.backend.application.ammunition.model.AmmunitionManufacturer;
import com.shottrack.backend.application.weapon.gateway.WeaponCaliberGateway;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AmmunitionService {

    private final AmmunitionGateway ammunitionGateway;
    private final AmmunitionManufacturerGateway ammunitionManufacturerGateway;
    private final WeaponCaliberGateway weaponCaliberGateway;
    private final AmmunitionMapper ammunitionMapper;

    public AmmunitionResponse register(UUID userId, AmmunitionRegisterRequest request) {
        if (request.manufacturerId() == null && (request.nickname() == null || request.nickname().isBlank())) {
            throw new BusinessException("AMMUNITION_IDENTIFICATION_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        AmmunitionManufacturer manufacturer = findManufacturerIfInformed(request.manufacturerId());
        WeaponCaliber caliber = findCaliberIfInformed(request.caliberId());

        Ammunition ammunition = new Ammunition(userId);
        ammunition.setManufacturerId(request.manufacturerId());
        ammunition.setCaliberId(request.caliberId());
        ammunition.setNickname(request.nickname());
        ammunition.setProjectileWeightGrains(request.projectileWeightGrains());
        ammunition.setPowderCharge(request.powderCharge());
        ammunition.setProjectileType(request.projectileType());
        ammunition.setLot(request.lot());
        ammunition.setNotes(request.notes());

        Ammunition saved = ammunitionGateway.save(ammunition);
        return ammunitionMapper.toResponse(saved, manufacturer, caliber);
    }

    public List<AmmunitionResponse> listByUser(UUID userId) {
        return ammunitionGateway.findAllByUserId(userId).stream()
                .map(this::toResponseWithCatalog)
                .toList();
    }

    private AmmunitionResponse toResponseWithCatalog(Ammunition ammunition) {
        AmmunitionManufacturer manufacturer = ammunition.getManufacturerId() == null ? null
                : ammunitionManufacturerGateway.findById(ammunition.getManufacturerId()).orElseThrow();
        WeaponCaliber caliber = ammunition.getCaliberId() == null ? null
                : weaponCaliberGateway.findById(ammunition.getCaliberId()).orElseThrow();
        return ammunitionMapper.toResponse(ammunition, manufacturer, caliber);
    }

    private AmmunitionManufacturer findManufacturerIfInformed(UUID manufacturerId) {
        if (manufacturerId == null) {
            return null;
        }
        return ammunitionManufacturerGateway.findById(manufacturerId)
                .orElseThrow(() -> new BusinessException("AMMUNITION_MANUFACTURER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private WeaponCaliber findCaliberIfInformed(UUID caliberId) {
        if (caliberId == null) {
            return null;
        }
        return weaponCaliberGateway.findById(caliberId)
                .orElseThrow(() -> new BusinessException("WEAPON_CALIBER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
