package com.shottrack.backend.common.security;

import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * ADR-0021: autoriza `/api/admin/**` só para ADMIN. O papel é lido do banco a cada
 * requisição (o JWT não carrega o papel), então promoção e rebaixamento valem na
 * requisição seguinte. Sem autenticação a decisão é negativa e o Spring Security
 * aciona o AuthenticationEntryPoint (401); autenticado sem ser admin cai no
 * AccessDeniedHandler (403 FORBIDDEN).
 */
@Component
@RequiredArgsConstructor
public class AdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final UserGateway userGateway;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UUID userId)) {
            return new AuthorizationDecision(false);
        }
        boolean admin = userGateway.findRoleById(userId).filter(Role.ADMIN::equals).isPresent();
        return new AuthorizationDecision(admin);
    }
}
