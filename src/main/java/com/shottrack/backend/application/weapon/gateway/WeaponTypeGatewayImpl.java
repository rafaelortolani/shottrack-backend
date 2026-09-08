package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.gateway.repository.WeaponTypeRepository;
import com.shottrack.backend.application.weapon.model.WeaponType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class WeaponTypeGatewayImpl implements WeaponTypeGateway {

    private final WeaponTypeRepository weaponTypeRepository;

    @Override
    public List<WeaponType> findAll() {
        return weaponTypeRepository.findAll();
    }

    @Override
    public Optional<WeaponType> findById(UUID id) {
        return weaponTypeRepository.findById(id);
    }
}
