package com.shottrack.backend.application.weapon.usecase;

import com.shottrack.backend.application.weapon.dto.WeaponBrandResponse;
import com.shottrack.backend.application.weapon.dto.WeaponCaliberResponse;
import com.shottrack.backend.application.weapon.dto.WeaponModelResponse;
import com.shottrack.backend.application.weapon.dto.WeaponTypeResponse;
import com.shottrack.backend.application.weapon.gateway.WeaponBrandGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponCaliberGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponModelGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponTypeGateway;
import com.shottrack.backend.application.weapon.mapper.WeaponMapper;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeaponCatalogService {

    private final WeaponTypeGateway weaponTypeGateway;
    private final WeaponBrandGateway weaponBrandGateway;
    private final WeaponModelGateway weaponModelGateway;
    private final WeaponCaliberGateway weaponCaliberGateway;
    private final WeaponMapper weaponMapper;

    public List<WeaponTypeResponse> listTypes() {
        return weaponTypeGateway.findAll().stream().map(weaponMapper::toResponse).toList();
    }

    public List<WeaponBrandResponse> listBrands() {
        return weaponBrandGateway.findAll().stream().map(weaponMapper::toResponse).toList();
    }

    public List<WeaponModelResponse> listModelsByBrand(UUID brandId) {
        if (!weaponBrandGateway.existsById(brandId)) {
            throw new BusinessException("WEAPON_BRAND_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        return weaponModelGateway.findAllByBrandId(brandId).stream().map(weaponMapper::toResponse).toList();
    }

    public List<WeaponCaliberResponse> listCalibers() {
        return weaponCaliberGateway.findAll().stream().map(weaponMapper::toResponse).toList();
    }
}
