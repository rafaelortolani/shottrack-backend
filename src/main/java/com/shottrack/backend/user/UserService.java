package com.shottrack.backend.user;

import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.user.dto.UserRegisterRequest;
import com.shottrack.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "EMAIL_ALREADY_REGISTERED",
                    "Já existe um usuário cadastrado com esse email",
                    HttpStatus.CONFLICT
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.name(), request.email(), passwordHash);
        User saved = userRepository.save(user);

        return UserResponse.from(saved);
    }
}
