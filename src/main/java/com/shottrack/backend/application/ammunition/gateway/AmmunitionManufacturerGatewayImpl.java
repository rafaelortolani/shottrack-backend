package com.shottrack.backend.application.ammunition.gateway;

import com.shottrack.backend.application.ammunition.gateway.repository.AmmunitionManufacturerRepository;
import com.shottrack.backend.application.ammunition.model.AmmunitionManufacturer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AmmunitionManufacturerGatewayImpl implements AmmunitionManufacturerGateway {

    private final AmmunitionManufacturerRepository ammunitionManufacturerRepository;

    @Override
    public Optional<AmmunitionManufacturer> findById(UUID id) {
        return ammunitionManufacturerRepository.findById(id);
    }
}
