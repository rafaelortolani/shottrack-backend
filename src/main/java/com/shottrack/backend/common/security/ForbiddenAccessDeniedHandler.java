package com.shottrack.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.IOException;
import java.util.Locale;

/**
 * ADR-0021: autenticado sem permissão para a rota (ex: não-admin em `/api/admin/**`).
 * Mesmo motivo do JwtAuthenticationEntryPoint: a negação acontece na cadeia de
 * filtros, antes do ApiExceptionHandler, então o envelope de erro é escrito aqui.
 */
@Component
@RequiredArgsConstructor
public class ForbiddenAccessDeniedHandler implements AccessDeniedHandler {

    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    // Fora do DispatcherServlet o LocaleContextHolder ainda não tem o locale da requisição.
    private final AcceptHeaderLocaleResolver localeResolver = newLocaleResolver();

    private static AcceptHeaderLocaleResolver newLocaleResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("pt"));
        return resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        String message = messageSource.getMessage("FORBIDDEN", null, localeResolver.resolveLocale(request));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("FORBIDDEN", message)));
    }
}
