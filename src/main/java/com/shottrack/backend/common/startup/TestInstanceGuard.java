package com.shottrack.backend.common.startup;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Set;

/**
 * Rodada 2: instância subida só para verificação (APP_TEST_INSTANCE=true) nunca pode apontar para o banco de
 * desenvolvimento (`shottrack`), para o do E2E do front (`shottrack_test`) nem para o vhost padrão do RabbitMQ (`/`).
 * Evita repetir o incidente em que DB_URL/RABBITMQ_* (que só valem no perfil prod) foram ignorados e a instância
 * gravou no banco de dev. Sem a variável, não faz nada.
 */
public class TestInstanceGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final Set<String> FORBIDDEN_DATABASES = Set.of("shottrack", "shottrack_test");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        check(context.getEnvironment());
    }

    static void check(ConfigurableEnvironment environment) {
        if (!environment.getProperty("app.test-instance", Boolean.class, false)) {
            return;
        }
        String database = databaseName(environment.getProperty("spring.datasource.url", ""));
        if (FORBIDDEN_DATABASES.contains(database)) {
            throw new IllegalStateException("APP_TEST_INSTANCE=true, mas a instância apontaria para o banco '" + database
                    + "'. Use SPRING_DATASOURCE_URL (DB_URL só vale no perfil prod); veja o README (Rodando os testes).");
        }
        if ("/".equals(environment.getProperty("spring.rabbitmq.virtual-host", "/"))) {
            throw new IllegalStateException("APP_TEST_INSTANCE=true, mas a instância usaria o vhost padrão '/' do RabbitMQ. "
                    + "Use SPRING_RABBITMQ_VIRTUAL_HOST; veja o README (Rodando os testes).");
        }
    }

    static String databaseName(String jdbcUrl) {
        String withoutQuery = jdbcUrl.split("\\?")[0];
        return withoutQuery.substring(withoutQuery.lastIndexOf('/') + 1);
    }
}
