package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.PendingRegistration;

import java.util.Optional;

public interface PendingRegistrationGateway {

    PendingRegistration save(PendingRegistration pendingRegistration);

    Optional<PendingRegistration> findByToken(String token);

    void invalidatePendingByEmail(String email);
}
