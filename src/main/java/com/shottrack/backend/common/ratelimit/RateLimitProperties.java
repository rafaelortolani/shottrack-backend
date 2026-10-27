package com.shottrack.backend.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * ADR-0018: limites configuráveis em app.rate-limit.* (application.yml).
 * Os defaults daqui espelham os valores iniciais do ADR.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean trustForwardedHeader,
        @DefaultValue Login login,
        @DefaultValue Registration registration,
        @DefaultValue Registration passwordResetRequest,
        @DefaultValue AccountDeletion accountDeletion,
        @DefaultValue DataExport dataExport
) {

    /** Tentativas de login por IP + e-mail na janela. */
    public record Login(
            @DefaultValue("10") int attempts,
            @DefaultValue("15m") Duration window
    ) {
    }

    /** ADR-0019 (UC52): tentativas de excluir a conta por usuário (ou IP, sem usuário) na janela. */
    public record AccountDeletion(
            @DefaultValue("5") int attempts,
            @DefaultValue("1h") Duration window
    ) {
    }

    /** ADR-0019 (UC53): exportações de dados por usuário (ou IP, sem usuário) na janela. */
    public record DataExport(
            @DefaultValue("3") int attempts,
            @DefaultValue("1h") Duration window
    ) {
    }

    /** Pedidos por e-mail e por IP na janela (cadastro e redefinição de senha). */
    public record Registration(
            @DefaultValue("5") int perEmail,
            @DefaultValue("20") int perIp,
            @DefaultValue("1h") Duration window
    ) {
    }
}
