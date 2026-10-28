package com.shottrack.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relógio injetável (UTC, o mesmo fuso das apurações por data — ADR-0022/0024), para
 * validar as datas da visita sem depender de Instant.now() espalhado.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
