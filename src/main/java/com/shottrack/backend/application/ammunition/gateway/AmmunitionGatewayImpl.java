package com.shottrack.backend.application.ammunition.gateway;

import com.shottrack.backend.application.ammunition.gateway.repository.AmmunitionRepository;
import com.shottrack.backend.application.ammunition.model.Ammunition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AmmunitionGatewayImpl implements AmmunitionGateway {

    private final AmmunitionRepository ammunitionRepository;

    @Override
    public Ammunition save(Ammunition ammunition) {
        return ammunitionRepository.save(ammunition);
    }
}
