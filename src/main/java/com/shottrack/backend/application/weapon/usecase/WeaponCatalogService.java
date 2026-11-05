package com.shottrack.backend.application.weapon.usecase;

import com.shottrack.backend.application.weapon.dto.WeaponBrandResponse;
import com.shottrack.backend.application.weapon.dto.WeaponCaliberResponse;
import com.shottrack.backend.application.weapon.dto.WeaponCatalogModelResponse;
import com.shottrack.backend.application.weapon.dto.WeaponTypeResponse;
import com.shottrack.backend.application.weapon.gateway.WeaponBrandGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponCaliberGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponModelGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponTypeGateway;
import com.shottrack.backend.application.weapon.mapper.WeaponMapper;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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

    /** Catálogo sempre em ordem alfabética (sem diferenciar maiúsculas de minúsculas), qualquer que seja a ordem do banco. */
    private static final Comparator<String> BY_NAME = String.CASE_INSENSITIVE_ORDER;

    public List<WeaponTypeResponse> listTypes() {
        return weaponTypeGateway.findAll().stream().map(weaponMapper::toResponse)
                .sorted(Comparator.comparing(WeaponTypeResponse::name, BY_NAME))
                .toList();
    }

    public List<WeaponBrandResponse> listBrands() {
        return weaponBrandGateway.findAll().stream().map(weaponMapper::toResponse)
                .sorted(Comparator.comparing(WeaponBrandResponse::name, BY_NAME))
                .toList();
    }

    /**
     * UC09: cada modelo já vem com seu tipo (ADR-0004, Revisão 2).
     */
    public List<WeaponCatalogModelResponse> listModelsByBrand(UUID brandId) {
        if (!weaponBrandGateway.existsById(brandId)) {
            throw new BusinessException("WEAPON_BRAND_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        return weaponModelGateway.findAllByBrandId(brandId).stream()
                .map(model -> weaponMapper.toCatalogResponse(model,
                        weaponTypeGateway.findById(model.getWeaponTypeId()).orElseThrow()))
                .sorted(Comparator.comparing(WeaponCatalogModelResponse::name, BY_NAME))
                .toList();
    }

    public List<WeaponCaliberResponse> listCalibers() {
        return weaponCaliberGateway.findAll().stream().map(weaponMapper::toResponse)
                .sorted(Comparator.comparing(WeaponCaliberResponse::name, BY_NAME))
                .toList();
    }

    /**
     * UC09 (fluxo 3): calibres válidos pra um modelo — é essa lista, não o
     * catálogo geral de calibres, que o cadastro de arma (UC06) usa.
     */
    public List<WeaponCaliberResponse> listCalibersByModel(UUID modelId) {
        WeaponModel model = weaponModelGateway.findById(modelId)
                .orElseThrow(() -> new BusinessException("WEAPON_MODEL_NOT_FOUND", HttpStatus.NOT_FOUND));

        return weaponCaliberGateway.findAllById(model.getCaliberIds()).stream()
                .map(weaponMapper::toResponse)
                .sorted(Comparator.comparing(WeaponCaliberResponse::name, BY_NAME))
                .toList();
    }
}
