package com.shottrack.backend.application.weapon.gateway;

import com.shottrack.backend.application.weapon.gateway.repository.WeaponRepository;
import com.shottrack.backend.application.weapon.model.Weapon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class WeaponGatewayImpl implements WeaponGateway {

    private final WeaponRepository weaponRepository;

    @Override
    public Weapon save(Weapon weapon) {
        return weaponRepository.save(weapon);
    }

    @Override
    public List<Weapon> findAllByUserId(UUID userId) {
        return weaponRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Weapon> findById(UUID id) {
        return weaponRepository.findById(id);
    }

    @Override
    public void delete(Weapon weapon) {
        weaponRepository.delete(weapon);
    }
}
