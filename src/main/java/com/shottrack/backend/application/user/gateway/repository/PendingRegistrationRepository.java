package com.shottrack.backend.application.user.gateway.repository;

import com.shottrack.backend.application.user.model.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, UUID> {

    Optional<PendingRegistration> findByToken(String token);

    List<PendingRegistration> findAllByEmailAndUsedFalse(String email);
}
