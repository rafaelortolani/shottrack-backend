package com.shottrack.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Habilita createdAt/updatedAt/createdBy/updatedBy automáticos em AbstractBaseEntity.
 * O autor é o usuário autenticado (o principal do JwtAuthenticationFilter é o UUID do
 * usuário); fora de uma requisição autenticada (cadastro, tarefas agendadas, consumidor
 * da fila) fica null. Necessário para a auditoria da administração (ADR-0023, UC54).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UUID userId) {
                return Optional.of(userId);
            }
            return Optional.empty();
        };
    }
}
