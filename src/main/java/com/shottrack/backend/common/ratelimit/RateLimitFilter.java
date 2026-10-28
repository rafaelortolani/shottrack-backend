package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.common.exception.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ADR-0018: rate limit em memória (Bucket4j) de login, cadastro, redefinição de senha,
 * renovação de sessão, troca de senha/e-mail, exclusão e exportação da conta. Fica fora dos usecases: roda como filtro, lê o e-mail
 * do corpo sem consumi-lo (CachedBodyRequest) e, quando excede, entrega o
 * TooManyRequestsException ao ApiExceptionHandler pra manter o envelope de
 * erro, a mensagem pt/en e o Retry-After num lugar só.
 *
 * Os buckets vivem num cache Caffeine que expira entradas inativas por mais
 * que a maior janela configurada — nesse ponto o bucket já estaria cheio de
 * novo, então recriá-lo é equivalente e a memória não cresce sem limite.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN = "login";
    private static final String REGISTRATION = "registration";
    private static final String PASSWORD_RESET = "password-reset";
    private static final String ACCOUNT_DELETION = "account-deletion";
    private static final String DATA_EXPORT = "data-export";
    private static final String REFRESH = "refresh";
    private static final String PASSWORD_RESET_CONFIRM = "password-reset-confirm";
    private static final String PASSWORD_CHANGE = "password-change";
    private static final String EMAIL_CHANGE = "email-change";
    private static final String EMAIL_CHANGE_CONFIRMATION = "email-change-confirmation";
    private static final String REGISTRATION_COMPLETION = "registration-completion";
    /** Chaves em minúsculas: o caminho é normalizado (decodificado, sem `;`, sem `//`) e minúsculo antes da busca. */
    private static final Map<String, String> RULE_BY_ENDPOINT = Map.ofEntries(
            Map.entry("POST /api/auth/login", LOGIN),
            Map.entry("POST /api/users/registration", REGISTRATION),
            Map.entry("POST /api/auth/password-reset/request", PASSWORD_RESET),
            Map.entry("POST /api/users/me/deletion", ACCOUNT_DELETION),
            Map.entry("POST /api/users/me/export", DATA_EXPORT),
            Map.entry("POST /api/auth/refresh", REFRESH),
            Map.entry("POST /api/auth/password-reset/confirm", PASSWORD_RESET_CONFIRM),
            Map.entry("PATCH /api/users/me/password", PASSWORD_CHANGE),
            Map.entry("POST /api/users/me/email", EMAIL_CHANGE),
            Map.entry("POST /api/users/me/email/confirmation", EMAIL_CHANGE_CONFIRMATION),
            Map.entry("POST /api/users/registration/completion", REGISTRATION_COMPLETION));
    /** Decodifica %xx e descarta `;param` e `//` — o mesmo caminho que o Spring MVC usa para rotear. */
    private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();
    /** Regras limitadas pelo e-mail do corpo; as demais são por usuário autenticado (ou IP). */
    private static final Set<String> RULES_KEYED_BY_BODY_EMAIL = Set.of(LOGIN, REGISTRATION, PASSWORD_RESET);

    /** Corpos de login/cadastro/e-mail são minúsculos; acima disso só vale o limite por IP. */
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private static final String FORWARDED_HEADER = "X-Forwarded-For";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper,
                           HandlerExceptionResolver handlerExceptionResolver) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.localeResolver.setDefaultLocale(Locale.forLanguageTag("pt"));
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(longestWindow(properties).toNanos(), TimeUnit.NANOSECONDS)
                .maximumSize(100_000)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || matchOf(request) == null;
    }

    /**
     * S01: a regra é escolhida pelo caminho NORMALIZADO (o que o roteador enxerga), não pela URL bruta —
     * `/api/auth/log%69n`, `/API/AUTH/LOGIN` ou `/api/auth/login/` caem na mesma regra (e no mesmo
     * bucket) que `/api/auth/login`. `variant` marca a requisição cujo caminho bruto difere do canônico.
     */
    private static Match matchOf(HttpServletRequest request) {
        String path;
        try {
            path = PATH_HELPER.getPathWithinApplication(request);
        } catch (RuntimeException e) {
            return null;
        }
        String canonical = path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        canonical = canonical.toLowerCase(Locale.ROOT);
        String rule = RULE_BY_ENDPOINT.get(request.getMethod() + " " + canonical);
        return rule == null ? null : new Match(rule, !request.getRequestURI().equals(canonical));
    }

    private record Match(String rule, boolean variant) {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Match match = matchOf(request);
        String rule = match.rule();
        HttpServletRequest forwarded = request;
        String subject;
        if (RULES_KEYED_BY_BODY_EMAIL.contains(rule)) {
            byte[] prefix = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            subject = prefix.length <= MAX_BODY_BYTES ? extractEmail(prefix) : null;
            forwarded = new CachedBodyRequest(request, prefix);
        } else {
            subject = authenticatedUserId();
        }

        try {
            consume(rule, clientIp(request), subject);
        } catch (TooManyRequestsException ex) {
            LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
            try {
                handlerExceptionResolver.resolveException(request, response, null, ex);
            } finally {
                LocaleContextHolder.resetLocaleContext();
            }
            return;
        }

        if (match.variant()) {
            // S01: o limite já foi cobrado (mesmo bucket do caminho canônico); a variação em si nunca
            // chega ao controller.
            LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
            try {
                handlerExceptionResolver.resolveException(request, response, null,
                        new BusinessException("INVALID_REQUEST_PATH", HttpStatus.BAD_REQUEST));
            } finally {
                LocaleContextHolder.resetLocaleContext();
            }
            return;
        }

        chain.doFilter(forwarded, response);
    }

    private void consume(String rule, String ip, String subject) {
        for (Limit limit : limitsFor(rule, ip, subject)) {
            Bucket bucket = buckets.get(limit.key(), key -> newBucket(limit));
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long seconds = Math.max(1, (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000d));
                throw new TooManyRequestsException(seconds);
            }
        }
    }

    /**
     * Sem e-mail legível no corpo, só entra o limite por IP (no login, a chave
     * IP + e-mail degenera pra só IP).
     */
    private List<Limit> limitsFor(String rule, String ip, String subject) {
        List<Limit> limits = new ArrayList<>();
        switch (rule) {
            case LOGIN -> {
                limits.add(new Limit("login:" + ip + "|" + (subject == null ? "" : subject),
                        properties.login().attempts(), properties.login().window()));
                // S14: teto por e-mail, independente do IP (e-mail já em minúsculas)
                if (subject != null) {
                    limits.add(new Limit("login-email:" + subject,
                            properties.login().perEmail(), properties.login().perEmailWindow()));
                }
            }
            case REGISTRATION -> addEmailAndIpLimits(limits, rule, properties.registration(), ip, subject);
            case PASSWORD_RESET -> addEmailAndIpLimits(limits, rule, properties.passwordResetRequest(), ip, subject);
            case ACCOUNT_DELETION -> addUserOrIpLimit(limits, rule, ip, subject,
                    properties.accountDeletion().attempts(), properties.accountDeletion().window());
            case DATA_EXPORT -> addUserOrIpLimit(limits, rule, ip, subject,
                    properties.dataExport().attempts(), properties.dataExport().window());
            case PASSWORD_CHANGE -> addUserOrIpLimit(limits, rule, ip, subject,
                    properties.passwordChange().attempts(), properties.passwordChange().window());
            case EMAIL_CHANGE -> addUserOrIpLimit(limits, rule, ip, subject,
                    properties.emailChange().attempts(), properties.emailChange().window());
            case EMAIL_CHANGE_CONFIRMATION -> addUserOrIpLimit(limits, rule, ip, subject,
                    properties.emailChangeConfirmation().attempts(), properties.emailChangeConfirmation().window());
            case REFRESH -> limits.add(new Limit(rule + ":ip:" + ip,
                    properties.refresh().attempts(), properties.refresh().window()));
            case PASSWORD_RESET_CONFIRM -> limits.add(new Limit(rule + ":ip:" + ip,
                    properties.passwordResetConfirm().attempts(), properties.passwordResetConfirm().window()));
            case REGISTRATION_COMPLETION -> limits.add(new Limit(rule + ":ip:" + ip,
                    properties.registrationCompletion().attempts(), properties.registrationCompletion().window()));
            default -> throw new IllegalStateException("Regra de rate limit desconhecida: " + rule);
        }
        return limits;
    }

    /** Por usuário autenticado; sem usuário válido no token, cai para o IP. */
    private void addUserOrIpLimit(List<Limit> limits, String rule, String ip, String userId, int attempts, Duration window) {
        limits.add(new Limit(rule + ":" + (userId != null ? "user:" + userId : "ip:" + ip), attempts, window));
    }

    private void addEmailAndIpLimits(List<Limit> limits, String rule, RateLimitProperties.Registration config,
                                     String ip, String email) {
        if (email != null) {
            limits.add(new Limit(rule + ":email:" + email, config.perEmail(), config.window()));
        }
        limits.add(new Limit(rule + ":ip:" + ip, config.perIp(), config.window()));
    }

    private Bucket newBucket(Limit limit) {
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth.capacity(limit.capacity())
                        .refillIntervally(limit.capacity(), limit.window()))
                .build();
    }

    /**
     * O filtro roda depois da cadeia do Spring Security, então o usuário do
     * token já está no contexto; sem token válido o limite cai pro IP.
     */
    private String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UUID userId
                ? userId.toString()
                : null;
    }

    private String extractEmail(byte[] body) {
        try {
            JsonNode email = objectMapper.readTree(body).path("email");
            if (!email.isTextual() || email.asText().isBlank()) {
                return null;
            }
            return email.asText().trim().toLowerCase(Locale.ROOT);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * X-Forwarded-For só vale com app.rate-limit.trust-forwarded-header=true (atrás do proxy); senão
     * qualquer um furaria o limite forjando o header. S14: o proxy ACRESCENTA o endereço que viu ao fim
     * da lista, então o cliente real é a entrada a `trusted-proxy-hops` posições do fim (padrão 1 = a
     * última) — as anteriores foram escritas pelo próprio cliente e não são confiáveis. Lista menor que
     * o esperado = header forjado/ausente → vale o IP da conexão.
     */
    private String clientIp(HttpServletRequest request) {
        if (properties.trustForwardedHeader()) {
            List<String> entries = new ArrayList<>();
            for (String header : Collections.list(request.getHeaders(FORWARDED_HEADER))) {
                for (String entry : header.split(",")) {
                    if (!entry.isBlank()) {
                        entries.add(entry.trim());
                    }
                }
            }
            int hops = Math.max(1, properties.trustedProxyHops());
            if (entries.size() >= hops) {
                return entries.get(entries.size() - hops);
            }
        }
        return request.getRemoteAddr();
    }

    private static Duration longestWindow(RateLimitProperties properties) {
        return List.of(properties.login().window(), properties.login().perEmailWindow(),
                        properties.registration().window(), properties.passwordResetRequest().window(),
                        properties.accountDeletion().window(), properties.dataExport().window(),
                        properties.refresh().window(), properties.passwordResetConfirm().window(),
                        properties.passwordChange().window(), properties.emailChange().window(),
                        properties.emailChangeConfirmation().window(), properties.registrationCompletion().window()).stream()
                .max(Duration::compareTo)
                .orElseThrow();
    }

    private record Limit(String key, int capacity, Duration window) {
    }
}
