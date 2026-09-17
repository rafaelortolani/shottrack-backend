package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.model.PendingRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PendingRegistrationGatewayImpl implements PendingRegistrationGateway {

    private final PendingRegistrationRepository pendingRegistrationRepository;

    @Override
    public PendingRegistration save(PendingRegistration pendingRegistration) {
        return pendingRegistrationRepository.save(pendingRegistration);
    }

    @Override
    public Optional<PendingRegistration> findByToken(String token) {
        return pendingRegistrationRepository.findByToken(token);
    }

    @Override
    public void invalidatePendingByEmail(String email) {
        List<PendingRegistration> pending = pendingRegistrationRepository.findAllByEmailAndUsedFalse(email);
        pending.forEach(PendingRegistration::markUsed);
        pendingRegistrationRepository.saveAll(pending);
    }
}
