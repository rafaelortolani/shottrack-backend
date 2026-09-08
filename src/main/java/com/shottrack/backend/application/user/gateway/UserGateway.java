package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserGateway {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    User save(User user);
}
