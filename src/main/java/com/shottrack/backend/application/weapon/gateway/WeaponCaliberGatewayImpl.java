package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.gateway.repository.WeaponCaliberRepository;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class WeaponCaliberGatewayImpl implements WeaponCaliberGateway {

    private final WeaponCaliberRepository weaponCaliberRepository;

    @Override
    public List<WeaponCaliber> findAll() {
        return weaponCaliberRepository.findAll();
    }

    @Override
    public Optional<WeaponCaliber> findById(UUID id) {
        return weaponCaliberRepository.findById(id);
    }
}
