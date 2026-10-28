package com.shottrack.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.IOException;
import java.util.Locale;

/**
 * Endpoint protegido chamado sem token, com token ausente/inválido não capturado
 * pelo JwtAuthenticationFilter — resposta no mesmo formato de erro padrão da API
 * (ApiExceptionHandler não entra em jogo aqui, pois a rejeição acontece na
 * cadeia de filtros do Spring Security, antes do DispatcherServlet).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

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
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String message = messageSource.getMessage("UNAUTHORIZED", null, localeResolver.resolveLocale(request));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("UNAUTHORIZED", message)));
    }
}
