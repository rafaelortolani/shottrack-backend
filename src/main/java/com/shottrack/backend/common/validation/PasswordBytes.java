package com.shottrack.backend.common.validation;

import com.shottrack.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

/**
 * N2 (defesa em profundidade): o BCrypt recusa senha com mais de 72 bytes UTF-8 (IllegalArgumentException).
 * A validação do DTO (@MaxUtf8Bytes) já barra, mas nenhum encode() pode virar 500 se ela for esquecida
 * numa rota nova: este ponto único devolve VALIDATION_ERROR (400).
 */
public final class PasswordBytes {

    public static final int MAX = 72;

    private PasswordBytes() {
    }

    public static String encode(PasswordEncoder encoder, String rawPassword) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        return encoder.encode(rawPassword);
    }
}
