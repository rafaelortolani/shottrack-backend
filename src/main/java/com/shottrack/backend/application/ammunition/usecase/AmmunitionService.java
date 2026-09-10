package com.shottrack.backend.application.ammunition.usecase;

import com.shottrack.backend.application.ammunition.dto.AmmunitionRegisterRequest;
import com.shottrack.backend.application.ammunition.dto.AmmunitionResponse;
import com.shottrack.backend.application.ammunition.dto.AmmunitionUpdateRequest;
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

        Ammunition ammunition = Ammunition.builder()
                .userId(userId)
                .manufacturerId(request.manufacturerId())
                .caliberId(request.caliberId())
                .nickname(request.nickname())
                .projectileWeightGrains(request.projectileWeightGrains())
                .powderCharge(request.powderCharge())
                .projectileType(request.projectileType())
                .lot(request.lot())
                .notes(request.notes())
                .build();

        Ammunition saved = ammunitionGateway.save(ammunition);
        return ammunitionMapper.toResponse(saved, manufacturer, caliber);
    }

    public List<AmmunitionResponse> listByUser(UUID userId) {
        return ammunitionGateway.findAllByUserId(userId).stream()
                .map(this::toResponseWithCatalog)
                .toList();
    }

    /**
     * UC15: edição parcial (diferente de arma) — só os campos enviados (não nulos)
     * são aplicados. A regra "fabricante OU apelido" do UC13 é revalidada contra o
     * estado final (campo enviado, ou o que já existia se não foi enviado).
     */
    public AmmunitionResponse update(UUID userId, UUID ammunitionId, AmmunitionUpdateRequest request) {
        Ammunition ammunition = findOwnedAmmunitionOrThrow(userId, ammunitionId);

        if (request.manufacturerId() != null) {
            findManufacturerIfInformed(request.manufacturerId());
        }
        if (request.caliberId() != null) {
            findCaliberIfInformed(request.caliberId());
        }

        UUID finalManufacturerId = request.manufacturerId() != null ? request.manufacturerId() : ammunition.getManufacturerId();
        String finalNickname = request.nickname() != null ? request.nickname() : ammunition.getNickname();
        if (finalManufacturerId == null && (finalNickname == null || finalNickname.isBlank())) {
            throw new BusinessException("AMMUNITION_IDENTIFICATION_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        if (request.manufacturerId() != null) {
            ammunition.setManufacturerId(request.manufacturerId());
        }
        if (request.caliberId() != null) {
            ammunition.setCaliberId(request.caliberId());
        }
        if (request.nickname() != null) {
            ammunition.setNickname(request.nickname());
        }
        if (request.projectileWeightGrains() != null) {
            ammunition.setProjectileWeightGrains(request.projectileWeightGrains());
        }
        if (request.powderCharge() != null) {
            ammunition.setPowderCharge(request.powderCharge());
        }
        if (request.projectileType() != null) {
            ammunition.setProjectileType(request.projectileType());
        }
        if (request.lot() != null) {
            ammunition.setLot(request.lot());
        }
        if (request.notes() != null) {
            ammunition.setNotes(request.notes());
        }

        Ammunition saved = ammunitionGateway.save(ammunition);
        return toResponseWithCatalog(saved);
    }

    private Ammunition findOwnedAmmunitionOrThrow(UUID userId, UUID ammunitionId) {
        return ammunitionGateway.findById(ammunitionId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("AMMUNITION_NOT_FOUND", HttpStatus.NOT_FOUND));
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
