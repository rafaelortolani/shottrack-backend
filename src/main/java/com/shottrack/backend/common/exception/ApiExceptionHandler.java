package com.shottrack.backend.common.exception;

import com.shottrack.backend.common.web.ApiResponse;
import com.shottrack.backend.common.web.RequestBodyTooLargeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex) {
        String message = messageSource.getMessage(ex.getCode(), null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), message));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<Object>> handleTooManyRequests(TooManyRequestsException ex) {
        String message = messageSource.getMessage(ex.getCode(), null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ApiResponse.error(ex.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                // ADR-0019: falha de conversão (ex: UUID inválido) traz o valor recebido na
                // mensagem padrão — pode ser dado pessoal, então não vai pra resposta.
                .map(f -> f.getField() + ": " + (f.isBindingFailure() ? "valor inválido" : f.getDefaultMessage()))
                .orElse("Dados inválidos");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /**
     * ADR-0019: corpo ilegível ou parâmetro com tipo errado (ex: UUID inválido).
     * A mensagem da exceção ecoa o valor recebido — pode ser dado pessoal — então
     * nem vai pra resposta nem pro log (e não é um erro nosso, não vira evento
     * no Sentry).
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Object>> handleUnreadableInput(Exception ex) {
        // S10: corpo sem Content-Length que passou do teto — o stream do filtro interrompeu a leitura
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof RequestBodyTooLargeException) {
                String message = messageSource.getMessage("PAYLOAD_TOO_LARGE", null, LocaleContextHolder.getLocale());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(ApiResponse.error("PAYLOAD_TOO_LARGE", message));
            }
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "Dados inválidos"));
    }

    /**
     * S08: erro de quem chama (parâmetro obrigatório ausente), não nosso — 400 sem log ERROR
     * (que viraria evento no Sentry). O nome do parâmetro é fixo no código, não ecoa dado do usuário.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.debug("Parâmetro obrigatório ausente: {}", ex.getParameterName());
        return clientError(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER");
    }

    /** S08: método HTTP errado numa rota que existe — 405 com o cabeçalho Allow. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.debug("Método não suportado: {}", ex.getMethod());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (ex.getSupportedHttpMethods() != null) {
            response.allow(ex.getSupportedHttpMethods().toArray(new org.springframework.http.HttpMethod[0]));
        }
        return response.body(error("METHOD_NOT_ALLOWED"));
    }

    /** ADR-0026: arquivo acima do limite do Spring (multipart.max-file-size / max-request-size) — 413. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.debug("Upload acima do limite");
        return clientError(HttpStatus.PAYLOAD_TOO_LARGE, "PHOTO_TOO_LARGE");
    }

    /** ADR-0026: campo `file` ausente ou corpo multipart malformado — erro de quem chama, 400 sem log ERROR. */
    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadMultipart(Exception ex) {
        log.debug("Multipart inválido: {}", ex.getClass().getSimpleName());
        return clientError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    /** S08: Content-Type que a rota não aceita (ex: text/plain) — 415. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.debug("Content-Type não suportado: {}", ex.getContentType());
        return clientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE");
    }

    /** S08: rota inexistente — 404, não 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException ex) {
        log.debug("Rota inexistente: {}", ex.getResourcePath());
        return clientError(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND");
    }

    private ResponseEntity<ApiResponse<Object>> clientError(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(error(code));
    }

    private ApiResponse<Object> error(String code) {
        return ApiResponse.error(code, messageSource.getMessage(code, null, LocaleContextHolder.getLocale()));
    }

    /**
     * ADR-0017: o log ERROR é o que leva o erro pro arquivo de log e pro
     * Sentry (via sentry-logback) — como toda Exception é tratada aqui, a
     * captura automática de exception não tratada do starter nunca dispara.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Erro interno inesperado"));
    }
}
