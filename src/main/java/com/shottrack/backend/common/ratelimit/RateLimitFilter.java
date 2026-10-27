package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shottrack.backend.common.exception.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ADR-0018: rate limit em memória (Bucket4j) de login, cadastro e pedido de
 * redefinição de senha. Fica fora dos usecases: roda como filtro, lê o e-mail
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

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTRATION_PATH = "/api/users/registration";
    private static final String PASSWORD_RESET_PATH = "/api/auth/password-reset/request";
    private static final Map<String, String> RULE_BY_PATH = Map.of(
            LOGIN_PATH, "login",
            REGISTRATION_PATH, "registration",
            PASSWORD_RESET_PATH, "password-reset");

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
        return !properties.enabled()
                || !"POST".equals(request.getMethod())
                || !RULE_BY_PATH.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] prefix = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        String email = prefix.length <= MAX_BODY_BYTES ? extractEmail(prefix) : null;
        HttpServletRequest forwarded = new CachedBodyRequest(request, prefix);

        try {
            consume(RULE_BY_PATH.get(request.getRequestURI()), clientIp(request), email);
        } catch (TooManyRequestsException ex) {
            LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
            try {
                handlerExceptionResolver.resolveException(request, response, null, ex);
            } finally {
                LocaleContextHolder.resetLocaleContext();
            }
            return;
        }

        chain.doFilter(forwarded, response);
    }

    private void consume(String rule, String ip, String email) {
        for (Limit limit : limitsFor(rule, ip, email)) {
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
    private List<Limit> limitsFor(String rule, String ip, String email) {
        List<Limit> limits = new ArrayList<>();
        switch (rule) {
            case "login" -> limits.add(new Limit("login:" + ip + "|" + (email == null ? "" : email),
                    properties.login().attempts(), properties.login().window()));
            case "registration" -> addEmailAndIpLimits(limits, rule, properties.registration(), ip, email);
            default -> addEmailAndIpLimits(limits, rule, properties.passwordResetRequest(), ip, email);
        }
        return limits;
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
     * X-Forwarded-For só vale com app.rate-limit.trust-forwarded-header=true
     * (atrás do proxy); senão qualquer um furaria o limite forjando o header.
     */
    private String clientIp(HttpServletRequest request) {
        if (properties.trustForwardedHeader()) {
            String header = request.getHeader(FORWARDED_HEADER);
            if (header != null && !header.isBlank()) {
                return header.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static Duration longestWindow(RateLimitProperties properties) {
        return List.of(properties.login().window(), properties.registration().window(),
                        properties.passwordResetRequest().window()).stream()
                .max(Duration::compareTo)
                .orElseThrow();
    }

    private record Limit(String key, int capacity, Duration window) {
    }
}
