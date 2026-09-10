package com.shottrack.backend.application.ammunition.gateway;

import com.shottrack.backend.application.ammunition.gateway.repository.AmmunitionRepository;
import com.shottrack.backend.application.ammunition.model.Ammunition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AmmunitionGatewayImpl implements AmmunitionGateway {

    private final AmmunitionRepository ammunitionRepository;

    @Override
    public Ammunition save(Ammunition ammunition) {
        return ammunitionRepository.save(ammunition);
    }

    @Override
    public List<Ammunition> findAllByUserId(UUID userId) {
        return ammunitionRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Ammunition> findById(UUID id) {
        return ammunitionRepository.findById(id);
    }
}
