package com.shottrack.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception base para regras de negócio violadas.
 * Sempre carrega um código estável (pra frontend tratar e pra resolver a mensagem
 * traduzida em messages.properties) e o status HTTP correto.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
    }
}
