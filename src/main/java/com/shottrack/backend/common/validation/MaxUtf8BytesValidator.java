package com.shottrack.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, CharSequence> {

    private int max;

    @Override
    public void initialize(MaxUtf8Bytes annotation) {
        this.max = annotation.value();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        // cada char gera no máximo 3 bytes em UTF-8: só vale codificar quando o tamanho ainda pode estourar
        return value.length() * 3 <= max
                || value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
