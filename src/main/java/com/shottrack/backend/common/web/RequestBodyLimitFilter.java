package com.shottrack.backend.common.web;

import com.shottrack.backend.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * S10: teto para o corpo das requisições (`app.request.max-body-bytes`, padrão 64 KB — a API só recebe
 * JSON pequeno). Com Content-Length conhecido a recusa é imediata (413, antes de ler qualquer byte); sem
 * ele (chunked) o stream conta os bytes e interrompe ao passar do teto. Roda antes do Spring Security,
 * então corpos enormes nem chegam a login/rate limit. A resposta usa o envelope de erro padrão (pt/en).
 */
@Component
@Order(-200)
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    private static final java.util.regex.Pattern PHOTO_UPLOAD_PATH = java.util.regex.Pattern.compile("^/api/series/[^/]+/photos/?$");

    private final long maxBodyBytes;
    private final long maxUploadBodyBytes;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

    public RequestBodyLimitFilter(@Value("${app.request.max-body-bytes:65536}") long maxBodyBytes,
                                  @Value("${app.request.max-upload-body-bytes:11534336}") long maxUploadBodyBytes,
                                  HandlerExceptionResolver handlerExceptionResolver) {
        this.maxBodyBytes = maxBodyBytes;
        this.maxUploadBodyBytes = maxUploadBodyBytes;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.localeResolver.setDefaultLocale(Locale.forLanguageTag("pt"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long limit = isPhotoUpload(request) ? maxUploadBodyBytes : maxBodyBytes;
        if (request.getContentLengthLong() > limit) {
            LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
            try {
                handlerExceptionResolver.resolveException(request, response, null,
                        new BusinessException("PAYLOAD_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE));
            } finally {
                LocaleContextHolder.resetLocaleContext();
            }
            return;
        }
        chain.doFilter(request.getContentLengthLong() < 0 ? new LimitedBodyRequest(request, limit) : request, response);
    }

    /** ADR-0026: o único corpo grande da API é o upload de foto do alvo (multipart). */
    private static boolean isPhotoUpload(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && PHOTO_UPLOAD_PATH.matcher(request.getRequestURI().toLowerCase(Locale.ROOT)).matches();
    }

    /** Conta os bytes lidos de um corpo sem Content-Length e interrompe ao passar do teto. */
    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {

        private final long maxBytes;

        LimitedBodyRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream delegate = super.getInputStream();
            return new ServletInputStream() {
                private long read;

                private void count(long bytes) throws IOException {
                    read += bytes;
                    if (read > maxBytes) {
                        throw new RequestBodyTooLargeException(maxBytes);
                    }
                }

                @Override
                public int read() throws IOException {
                    int value = delegate.read();
                    if (value != -1) {
                        count(1);
                    }
                    return value;
                }

                @Override
                public int read(byte[] buffer, int off, int len) throws IOException {
                    int count = delegate.read(buffer, off, len);
                    if (count > 0) {
                        count(count);
                    }
                    return count;
                }

                @Override
                public boolean isFinished() {
                    return delegate.isFinished();
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    delegate.setReadListener(readListener);
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
