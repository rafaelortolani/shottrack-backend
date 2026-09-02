package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.User;

public interface UserGateway {

    boolean existsByEmail(String email);

    User save(User user);
}
