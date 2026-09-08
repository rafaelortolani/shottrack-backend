package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.gateway.repository.WeaponBrandRepository;
import com.shottrack.backend.application.weapon.model.WeaponBrand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class WeaponBrandGatewayImpl implements WeaponBrandGateway {

    private final WeaponBrandRepository weaponBrandRepository;

    @Override
    public List<WeaponBrand> findAll() {
        return weaponBrandRepository.findAll();
    }

    @Override
    public Optional<WeaponBrand> findById(UUID id) {
        return weaponBrandRepository.findById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return weaponBrandRepository.existsById(id);
    }
}
