package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.gateway.repository.AccessoryRepository;
import com.shottrack.backend.application.accessory.model.Accessory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AccessoryGatewayImpl implements AccessoryGateway {

    private final AccessoryRepository accessoryRepository;

    @Override
    public Accessory save(Accessory accessory) {
        return accessoryRepository.save(accessory);
    }

    @Override
    public List<Accessory> findAllByUserId(UUID userId) {
        return accessoryRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Accessory> findById(UUID id) {
        return accessoryRepository.findById(id);
    }

    @Override
    public void delete(Accessory accessory) {
        accessoryRepository.delete(accessory);
    }
}
