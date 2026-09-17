package com.shottrack.backend.application.accessory.usecase;

import com.shottrack.backend.application.accessory.dto.AccessoryTypeResponse;
import com.shottrack.backend.application.accessory.gateway.AccessoryTypeGateway;
import com.shottrack.backend.application.accessory.mapper.AccessoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessoryCatalogService {

    private final AccessoryTypeGateway accessoryTypeGateway;
    private final AccessoryMapper accessoryMapper;

    public List<AccessoryTypeResponse> listTypes() {
        return accessoryTypeGateway.findAll().stream().map(accessoryMapper::toResponse).toList();
    }
}
