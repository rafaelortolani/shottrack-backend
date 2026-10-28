package com.shottrack.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ADR-0018: limite de requisições excedido. Carrega o tempo de espera pra o
 * ApiExceptionHandler devolver o header Retry-After.
 */
@Getter
public class TooManyRequestsException extends BusinessException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super("TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
