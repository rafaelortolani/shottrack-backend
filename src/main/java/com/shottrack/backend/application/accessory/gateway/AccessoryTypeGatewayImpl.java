package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.gateway.repository.AccessoryTypeRepository;
import com.shottrack.backend.application.accessory.model.AccessoryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AccessoryTypeGatewayImpl implements AccessoryTypeGateway {

    private final AccessoryTypeRepository accessoryTypeRepository;

    @Override
    public Optional<AccessoryType> findById(UUID id) {
        return accessoryTypeRepository.findById(id);
    }

    @Override
    public List<AccessoryType> findAll() {
        return accessoryTypeRepository.findAll();
    }
}
