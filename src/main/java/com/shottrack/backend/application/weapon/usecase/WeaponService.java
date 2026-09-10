package com.shottrack.backend.application.weapon.usecase;

import com.shottrack.backend.application.weapon.dto.WeaponRegisterRequest;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import com.shottrack.backend.application.weapon.dto.WeaponUpdateRequest;
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
        CatalogSelection selection = resolveCatalogSelection(
                request.typeId(), request.brandId(), request.modelId(), request.caliberId());

        Weapon weapon = Weapon.builder()
                .userId(userId)
                .typeId(selection.type().getId())
                .brandId(selection.brand().getId())
                .modelId(selection.model().getId())
                .caliberId(selection.caliber().getId())
                .nickname(request.nickname())
                .build();

        Weapon saved = weaponGateway.save(weapon);
        return selection.toResponse(weaponMapper, saved);
    }

    public List<WeaponResponse> listByUser(UUID userId) {
        return weaponGateway.findAllByUserId(userId).stream()
                .map(this::toResponseWithCatalog)
                .toList();
    }

    /**
     * UC10: o apelido pode ser alterado a qualquer momento. Já a troca de
     * tipo/marca/modelo/calibre de uma arma já usada em treino/resultado
     * deveria ser bloqueada (mesma razão do UC08) — mas, como esses domínios
     * ainda não existem, essa checagem ainda não tem o que checar.
     */
    public WeaponResponse update(UUID userId, UUID weaponId, WeaponUpdateRequest request) {
        Weapon weapon = findOwnedWeaponOrThrow(userId, weaponId);

        CatalogSelection selection = resolveCatalogSelection(
                request.typeId(), request.brandId(), request.modelId(), request.caliberId());

        weapon.setTypeId(selection.type().getId());
        weapon.setBrandId(selection.brand().getId());
        weapon.setModelId(selection.model().getId());
        weapon.setCaliberId(selection.caliber().getId());
        weapon.setNickname(request.nickname());

        Weapon saved = weaponGateway.save(weapon);
        return selection.toResponse(weaponMapper, saved);
    }

    /**
     * UC08: só exclui se a arma nunca foi usada em nenhum treino/resultado.
     * Os domínios de Treino e Resultado ainda não existem, então essa checagem
     * ainda não tem o que checar — toda exclusão é permitida por enquanto.
     */
    public void delete(UUID userId, UUID weaponId) {
        weaponGateway.delete(findOwnedWeaponOrThrow(userId, weaponId));
    }

    private Weapon findOwnedWeaponOrThrow(UUID userId, UUID weaponId) {
        return weaponGateway.findById(weaponId)
                .filter(w -> w.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("WEAPON_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private CatalogSelection resolveCatalogSelection(UUID typeId, UUID brandId, UUID modelId, UUID caliberId) {
        WeaponType type = weaponTypeGateway.findById(typeId)
                .orElseThrow(() -> new BusinessException("WEAPON_TYPE_NOT_FOUND", HttpStatus.NOT_FOUND));
        WeaponBrand brand = weaponBrandGateway.findById(brandId)
                .orElseThrow(() -> new BusinessException("WEAPON_BRAND_NOT_FOUND", HttpStatus.NOT_FOUND));
        WeaponModel model = weaponModelGateway.findById(modelId)
                .orElseThrow(() -> new BusinessException("WEAPON_MODEL_NOT_FOUND", HttpStatus.NOT_FOUND));
        WeaponCaliber caliber = weaponCaliberGateway.findById(caliberId)
                .orElseThrow(() -> new BusinessException("WEAPON_CALIBER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!model.getBrandId().equals(brand.getId())) {
            throw new BusinessException("WEAPON_MODEL_BRAND_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        return new CatalogSelection(type, brand, model, caliber);
    }

    private WeaponResponse toResponseWithCatalog(Weapon weapon) {
        WeaponType type = weaponTypeGateway.findById(weapon.getTypeId()).orElseThrow();
        WeaponBrand brand = weaponBrandGateway.findById(weapon.getBrandId()).orElseThrow();
        WeaponModel model = weaponModelGateway.findById(weapon.getModelId()).orElseThrow();
        WeaponCaliber caliber = weaponCaliberGateway.findById(weapon.getCaliberId()).orElseThrow();
        return weaponMapper.toResponse(weapon, type, brand, model, caliber);
    }

    private record CatalogSelection(WeaponType type, WeaponBrand brand, WeaponModel model, WeaponCaliber caliber) {

        WeaponResponse toResponse(WeaponMapper mapper, Weapon weapon) {
            return mapper.toResponse(weapon, type, brand, model, caliber);
        }
    }
}
