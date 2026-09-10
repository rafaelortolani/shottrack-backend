package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.gateway.repository.AccessoryRepository;
import com.shottrack.backend.application.accessory.model.Accessory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AccessoryGatewayImpl implements AccessoryGateway {

    private final AccessoryRepository accessoryRepository;

    @Override
    public Accessory save(Accessory accessory) {
        return accessoryRepository.save(accessory);
    }
}
