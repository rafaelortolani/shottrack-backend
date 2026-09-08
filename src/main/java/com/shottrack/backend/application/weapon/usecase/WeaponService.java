package com.shottrack.backend.application.weapon.usecase;

import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import com.shottrack.backend.application.weapon.gateway.WeaponBrandGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponCaliberGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponModelGateway;
import com.shottrack.backend.application.weapon.gateway.WeaponTypeGateway;
import com.shottrack.backend.application.weapon.mapper.WeaponMapper;
import com.shottrack.backend.application.weapon.model.Weapon;
import com.shottrack.backend.application.weapon.model.WeaponBrand;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import com.shottrack.backend.application.weapon.model.WeaponType;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeaponService {

    private final WeaponGateway weaponGateway;
    private final WeaponTypeGateway weaponTypeGateway;
    private final WeaponBrandGateway weaponBrandGateway;
    private final WeaponModelGateway weaponModelGateway;
    private final WeaponCaliberGateway weaponCaliberGateway;
    private final WeaponMapper weaponMapper;

    public WeaponResponse register(UUID userId, WeaponRegisterRequest request) {
        WeaponType type = weaponTypeGateway.findById(request.typeId())
                .orElseThrow(() -> new BusinessException("WEAPON_TYPE_NOT_FOUND", HttpStatus.NOT_FOUND));
        WeaponBrand brand = weaponBrandGateway.findById(request.brandId())
                .orElseThrow(() -> new BusinessException("WEAPON_BRAND_NOT_FOUND", HttpStatus.NOT_FOUND));
        WeaponModel model = weaponModelGateway.findById(request.modelId())
                .orElseThrow(() -> new BusinessException("WEAPON_MODEL_NOT_FOUND", HttpStatus.NOT_FOUND));
        WeaponCaliber caliber = weaponCaliberGateway.findById(request.caliberId())
                .orElseThrow(() -> new BusinessException("WEAPON_CALIBER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!model.getBrandId().equals(brand.getId())) {
            throw new BusinessException("WEAPON_MODEL_BRAND_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        Weapon weapon = new Weapon(userId, type.getId(), brand.getId(), model.getId(), caliber.getId());
        Weapon saved = weaponGateway.save(weapon);

        return weaponMapper.toResponse(saved, type, brand, model, caliber);
    }

    public List<WeaponResponse> listByUser(UUID userId) {
        return weaponGateway.findAllByUserId(userId).stream()
                .map(this::toResponseWithCatalog)
                .toList();
    }

    /**
     * UC08: só exclui se a arma nunca foi usada em nenhum treino/resultado.
     * Os domínios de Treino e Resultado ainda não existem, então essa checagem
     * ainda não tem o que checar — toda exclusão é permitida por enquanto.
     */
    public void delete(UUID userId, UUID weaponId) {
        Weapon weapon = weaponGateway.findById(weaponId)
                .filter(w -> w.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("WEAPON_NOT_FOUND", HttpStatus.NOT_FOUND));

        weaponGateway.delete(weapon);
    }

    private WeaponResponse toResponseWithCatalog(Weapon weapon) {
        WeaponType type = weaponTypeGateway.findById(weapon.getTypeId()).orElseThrow();
        WeaponBrand brand = weaponBrandGateway.findById(weapon.getBrandId()).orElseThrow();
        WeaponModel model = weaponModelGateway.findById(weapon.getModelId()).orElseThrow();
        WeaponCaliber caliber = weaponCaliberGateway.findById(weapon.getCaliberId()).orElseThrow();
        return weaponMapper.toResponse(weapon, type, brand, model, caliber);
    }
}
