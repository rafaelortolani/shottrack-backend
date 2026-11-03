package com.shottrack.backend.common.validation;

import com.shottrack.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** N2 (defesa em profundidade): mesmo sem a validação do DTO, o encode nunca vira IllegalArgumentException/500. */
class PasswordBytesTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void shouldEncodeUpTo72Bytes() {
        assertThat(PasswordBytes.encode(encoder, "a".repeat(72))).startsWith("$2");
        assertThat(PasswordBytes.encode(encoder, "ç".repeat(36))).startsWith("$2");
    }

    @Test
    void shouldAnswerValidationErrorInsteadOfLettingBcryptThrow() {
        for (String tooLong : new String[]{"a".repeat(73), "ç".repeat(37), "ç".repeat(80)}) {
            assertThatThrownBy(() -> PasswordBytes.encode(encoder, tooLong))
                    .isInstanceOfSatisfying(BusinessException.class, e -> {
                        assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR");
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }
    }
}
