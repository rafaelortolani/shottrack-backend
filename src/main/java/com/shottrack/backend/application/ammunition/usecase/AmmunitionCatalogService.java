package com.shottrack.backend.application.ammunition.usecase;

import com.shottrack.backend.application.ammunition.dto.AmmunitionManufacturerResponse;
import com.shottrack.backend.application.ammunition.gateway.AmmunitionManufacturerGateway;
import com.shottrack.backend.application.ammunition.mapper.AmmunitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmmunitionCatalogService {

    private final AmmunitionManufacturerGateway ammunitionManufacturerGateway;
    private final AmmunitionMapper ammunitionMapper;

    public List<AmmunitionManufacturerResponse> listManufacturers() {
        return ammunitionManufacturerGateway.findAll().stream().map(ammunitionMapper::toResponse).toList();
    }
}
