package com.shottrack.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Config mínima. UC02 (login) já emite o JWT, mas ainda não existe um filtro que
 * valide o token e restrinja endpoints com ele — por enquanto tudo continua
 * público e CSRF desligado (API stateless). Restringir endpoints de negócio ao
 * Authorization: Bearer <token> é trabalho do próximo use case que precisar disso.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users", "/api/auth/**").permitAll()
                        .anyRequest().permitAll() // será restringido quando um endpoint exigir o JWT
                );
        return http.build();
    }
}
