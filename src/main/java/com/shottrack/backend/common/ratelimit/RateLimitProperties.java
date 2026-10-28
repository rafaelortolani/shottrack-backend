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
        /** Quantos proxies confiáveis estão na frente: o IP do cliente é a entrada de X-Forwarded-For a essa distância do fim. */
        @DefaultValue("1") int trustedProxyHops,
        @DefaultValue Login login,
        @DefaultValue Registration registration,
        @DefaultValue Registration passwordResetRequest,
        @DefaultValue AccountDeletion accountDeletion,
        @DefaultValue DataExport dataExport,
        @DefaultValue Refresh refresh,
        @DefaultValue PasswordResetConfirm passwordResetConfirm,
        @DefaultValue PasswordChange passwordChange,
        @DefaultValue EmailChange emailChange,
        @DefaultValue EmailChangeConfirmation emailChangeConfirmation,
        @DefaultValue RegistrationCompletion registrationCompletion
) {

    /**
     * Tentativas de login por IP + e-mail na janela e, além disso, por e-mail (em minúsculas) sem
     * olhar o IP — para barrar quem espalha as tentativas por muitos IPs (S14).
     */
    public record Login(
            @DefaultValue("10") int attempts,
            @DefaultValue("15m") Duration window,
            @DefaultValue("20") int perEmail,
            @DefaultValue("1h") Duration perEmailWindow
    ) {
    }

    /** S13: renovações de sessão por IP na janela. */
    public record Refresh(
            @DefaultValue("30") int attempts,
            @DefaultValue("15m") Duration window
    ) {
    }

    /** S13: confirmações de redefinição de senha por IP na janela. */
    public record PasswordResetConfirm(
            @DefaultValue("10") int attempts,
            @DefaultValue("15m") Duration window
    ) {
    }

    /** S13: trocas de senha por usuário (ou IP, sem usuário) na janela — barra adivinhar a senha atual. */
    public record PasswordChange(
            @DefaultValue("5") int attempts,
            @DefaultValue("15m") Duration window
    ) {
    }

    /** S13: pedidos de troca de e-mail por usuário (ou IP) na janela — barra e-mail em massa. */
    public record EmailChange(
            @DefaultValue("5") int attempts,
            @DefaultValue("1h") Duration window
    ) {
    }

    /** S13: confirmações do código de troca de e-mail por usuário (ou IP) na janela. */
    public record EmailChangeConfirmation(
            @DefaultValue("10") int attempts,
            @DefaultValue("15m") Duration window
    ) {
    }

    /** S13: conclusões de cadastro por IP na janela. */
    public record RegistrationCompletion(
            @DefaultValue("10") int attempts,
            @DefaultValue("1h") Duration window
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
