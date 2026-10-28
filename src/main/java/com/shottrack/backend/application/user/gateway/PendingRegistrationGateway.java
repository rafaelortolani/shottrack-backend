package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.PendingRegistration;

import java.util.Optional;
import java.time.Instant;

public interface PendingRegistrationGateway {

    PendingRegistration save(PendingRegistration pendingRegistration);

    Optional<PendingRegistration> findByToken(String token);

    void invalidatePendingByEmail(String email);

    void deleteAllByEmail(String email);
    long deleteByExpiresAtBefore(Instant cutoff);
}
