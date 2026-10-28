package com.shottrack.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Agendamento só ligado com app.cleanup.enabled=true — os testes não sobem
 * nenhum job em segundo plano.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.cleanup.enabled", havingValue = "true")
public class SchedulingConfig {
}
