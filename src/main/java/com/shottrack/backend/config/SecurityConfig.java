package com.shottrack.backend.config;

import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.common.security.JwtAuthenticationEntryPoint;
import com.shottrack.backend.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

/**
 * Stateless: CSRF desligado e sem sessão — a única forma de autenticar um endpoint
 * protegido é via Authorization: Bearer <token>, validado pelo JwtAuthenticationFilter
 * (ADR-0001). Rotas de cadastro/login/refresh continuam públicas.
 *
 * No profile dev, o DevAutoLoginFilter autentica toda requisição sem token com um
 * usuário fixo de teste — dá pra explorar os endpoints protegidos no Swagger sem
 * precisar logar manualmente. Um Bearer token real, se enviado, tem prioridade.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Só existe no profile dev — ver DevAutoLoginFilter.
     */
    @Bean
    @Profile("dev")
    public DevAutoLoginFilter devAutoLoginFilter(UserGateway userGateway, PasswordEncoder passwordEncoder) {
        return new DevAutoLoginFilter(userGateway, passwordEncoder);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Optional<DevAutoLoginFilter> devAutoLoginFilter)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users", "/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        devAutoLoginFilter.ifPresent(filter -> http.addFilterBefore(filter, JwtAuthenticationFilter.class));

        return http.build();
    }
}
