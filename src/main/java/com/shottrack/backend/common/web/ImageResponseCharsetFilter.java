package com.shottrack.backend.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ADR-0026: `server.servlet.encoding.force=true` (necessário para o JSON com acentos) faria o Tomcat anexar
 * `;charset=UTF-8` ao Content-Type de TODA resposta — inclusive à imagem, que deve sair só como `image/jpeg`.
 * Para as rotas de conteúdo/miniatura da foto este filtro (que roda antes do CharacterEncodingFilter) entrega uma
 * resposta que ignora o charset forçado.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ImageResponseCharsetFilter extends OncePerRequestFilter {

    private static final Pattern IMAGE_PATH = Pattern.compile("^/api/series/[^/]+/photos/[^/]+/(content|thumbnail)/?$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod()) || !IMAGE_PATH.matcher(request.getRequestURI().toLowerCase(Locale.ROOT)).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, new HttpServletResponseWrapper(response) {
            @Override
            public void setCharacterEncoding(String encoding) {
                // imagem não tem charset
            }
        });
    }
}
