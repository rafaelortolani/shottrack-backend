package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.gateway.repository.WeaponModelRepository;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class WeaponModelGatewayImpl implements WeaponModelGateway {

    private final WeaponModelRepository weaponModelRepository;

    @Override
    public List<WeaponModel> findAllByBrandId(UUID brandId) {
        return weaponModelRepository.findAllByBrandId(brandId);
    }

    @Override
    public Optional<WeaponModel> findById(UUID id) {
        return weaponModelRepository.findById(id);
    }
}
