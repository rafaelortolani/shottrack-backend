package com.shottrack.backend.application.accessory.usecase;

import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.gateway.AccessoryGateway;
import com.shottrack.backend.application.accessory.mapper.AccessoryMapper;
import com.shottrack.backend.application.accessory.model.Accessory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessoryService {

    private final AccessoryGateway accessoryGateway;
    private final AccessoryMapper accessoryMapper;

    public AccessoryResponse register(UUID userId, AccessoryRegisterRequest request) {
        Accessory accessory = Accessory.builder()
                .userId(userId)
                .name(request.name())
                .type(request.type())
                .notes(request.notes())
                .build();

        Accessory saved = accessoryGateway.save(accessory);
        return accessoryMapper.toResponse(saved);
    }
}
