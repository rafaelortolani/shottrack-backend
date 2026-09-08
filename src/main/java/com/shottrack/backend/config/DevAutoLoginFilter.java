package com.shottrack.backend.config;

import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Só existe no profile dev (SecurityConfig). Autentica toda requisição com um
 * usuário fixo de teste, criado sob demanda — evita ter que logar manualmente
 * pra testar endpoints protegidos no Swagger em desenvolvimento. Roda antes do
 * JwtAuthenticationFilter, que sobrescreve essa autenticação se a requisição
 * trouxer um Bearer token válido — um token real sempre tem prioridade.
 */
@RequiredArgsConstructor
class DevAutoLoginFilter extends OncePerRequestFilter {

    static final String DEV_USER_EMAIL = "dev@shottrack.com";
    private static final String DEV_USER_NAME = "Usuário Dev";
    private static final String DEV_USER_PASSWORD = "dev12345";

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;

    private volatile UUID devUserId;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authentication = new UsernamePasswordAuthenticationToken(devUserId(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private synchronized UUID devUserId() {
        if (devUserId == null) {
            User user = userGateway.findByEmail(DEV_USER_EMAIL)
                    .orElseGet(() -> userGateway.save(
                            new User(DEV_USER_NAME, DEV_USER_EMAIL, passwordEncoder.encode(DEV_USER_PASSWORD))));
            devUserId = user.getId();
        }
        return devUserId;
    }
}
