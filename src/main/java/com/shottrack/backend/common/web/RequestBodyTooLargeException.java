package com.shottrack.backend.common.web;

import java.io.IOException;

/**
 * S10: lançada pelo stream do corpo quando uma requisição SEM Content-Length (chunked) passa do teto.
 * O ApiExceptionHandler a transforma em 413 PAYLOAD_TOO_LARGE.
 */
public class RequestBodyTooLargeException extends IOException {

    public RequestBodyTooLargeException(long maxBytes) {
        super("Corpo da requisição acima de " + maxBytes + " bytes");
    }
}
