package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.application.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserGatewayImpl implements UserGateway {

    private final UserRepository userRepository;

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}
