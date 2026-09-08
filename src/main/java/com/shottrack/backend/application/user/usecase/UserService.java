package com.shottrack.backend.application.user.usecase;

import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.user.dto.UserResponse;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.mapper.UserMapper;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserGateway userGateway;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(UserRegisterRequest request) {
        if (userGateway.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.name(), request.email(), passwordHash);
        User saved = userGateway.save(user);

        return userMapper.toResponse(saved);
    }

    public UserResponse getProfile(UUID userId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED));

        return userMapper.toResponse(user);
    }
}
