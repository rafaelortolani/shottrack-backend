package com.shottrack.backend.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * N2: limite em BYTES UTF-8, não em caracteres. O BCrypt recusa senha com mais de 72 bytes
 * (IllegalArgumentException → 500), e letra acentuada ocupa 2 bytes, emoji 4: `@Size(max = 72)` deixava passar
 * uma senha de 40 "ç" (80 bytes). Nulo é válido (use @NotBlank para obrigar).
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "{VALIDATION_PASSWORD_TOO_LONG}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
