package com.shottrack.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

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

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String message = messageSource.getMessage("UNAUTHORIZED", null, LocaleContextHolder.getLocale());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("UNAUTHORIZED", message)));
    }
}
