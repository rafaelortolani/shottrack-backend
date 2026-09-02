package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.User;

import java.util.Optional;

public interface UserGateway {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}
