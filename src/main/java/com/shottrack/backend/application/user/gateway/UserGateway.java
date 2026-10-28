package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.Role;
import com.shottrack.backend.application.user.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserGateway {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    User save(User user);

    boolean existsById(UUID id);

    Optional<User> findByIdForUpdate(UUID id);

    /** Consulta escalar: sempre vai ao banco, nunca devolve papel de entidade em cache. */
    Optional<Role> findRoleById(UUID id);

    void deleteById(UUID id);
}
